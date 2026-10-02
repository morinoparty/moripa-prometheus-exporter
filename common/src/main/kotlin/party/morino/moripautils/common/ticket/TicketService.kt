/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.ticket

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import party.morino.moripautils.common.model.config.MoripaUtilsConfig
import party.morino.moripautils.common.model.config.TicketCategory
import party.morino.moripautils.common.model.ticket.Ticket
import party.morino.moripautils.common.model.ticket.TicketSubmission
import party.morino.moripautils.common.model.ticket.TicketSubmitResult
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * チケットの送信を取りまとめるサービス (Facade)
 *
 * 入力の検証 → 保存 → 通知 の順に処理する。UI (Dialog) や HTTP API はこのクラスだけを呼べばよい。
 * 通知先は Koin に [TicketNotifier] として登録されたものすべて (Observer) で、送信のたびに取り出す。
 * 通知は [notificationScope] で非同期に行い、送信結果は保存が終わった時点で返す
 * (Webhook が遅くてもプレイヤーへの受付メッセージを待たせず、二重送信を誘発しないため)。
 *
 * @param logger 通知の失敗などを記録するロガー
 * @param notificationScope 通知を実行するスコープ。プラグインの無効化時に [close] で停止する
 */
class TicketService(
    private val logger: Logger,
    private val notificationScope: CoroutineScope,
) : MoripaUtilsKoinComponent {
    private val config: MoripaUtilsConfig by inject()
    private val repository: TicketRepository by inject()

    /**
     * チケットを送信する
     *
     * @param playerUuid 送信したプレイヤーの UUID
     * @param playerName 送信したプレイヤーの名前
     * @param categoryId 選択されたカテゴリーの id
     * @param content 入力された本文 (前後の空白は取り除いて保存する)
     * @return 送信結果。入力に誤りがある場合は保存も通知も行わない。通知の完了は待たない
     */
    suspend fun submit(
        playerUuid: UUID,
        playerName: String,
        categoryId: String,
        content: String,
    ): TicketSubmitResult {
        val trimmedContent = content.trim()
        // Dialog の選択肢は設定から作るが、不正なクライアントや古い Dialog に備えてここでも引き当てる
        val category = config.ticket.categories.firstOrNull { it.id == categoryId }
            ?: return TicketSubmitResult.UnknownCategory(categoryId)
        // 検証に失敗した場合は副作用 (保存・通知) を起こさずに理由を返す
        validateContent(trimmedContent)?.let { return it }

        val ticket = repository.create(
            TicketSubmission(
                serverId = config.server,
                playerUuid = playerUuid,
                playerName = playerName,
                categoryId = category.id,
                content = trimmedContent,
            ),
        )
        notifyAll(ticket, category)
        return TicketSubmitResult.Success(ticket)
    }

    /**
     * 登録されているすべての通知先へ通知を開始する (完了は待たない)
     *
     * 1 つの通知先が失敗しても他の通知先やチケットの保存結果に影響しないよう、通知先ごとに例外を捕捉する。
     *
     * @param ticket 保存されたチケット
     * @param category チケットのカテゴリー
     */
    private fun notifyAll(ticket: Ticket, category: TicketCategory) {
        // Webhook の応答待ちでゲーム内通知が遅れないよう、通知先ごとに別のコルーチンで並行して送る
        getKoin().getAll<TicketNotifier>().forEach { notifier ->
            notificationScope.launch {
                try {
                    notifier.notify(ticket, category)
                } catch (e: CancellationException) {
                    // コルーチンのキャンセルは握りつぶさずに伝える
                    throw e
                } catch (e: Exception) {
                    // 通知先の実装は例外を投げない約束だが、念のためここでも保存結果を守る
                    val notifierName = notifier::class.simpleName
                    logger.log(Level.WARNING, "Ticket notifier $notifierName failed for ticket #${ticket.id}", e)
                }
            }
        }
    }

    /**
     * 実行中の通知を取り消し、以降の通知を行わないようにする (プラグインの無効化時に呼ぶ)
     */
    fun close() {
        notificationScope.cancel()
    }

    companion object {
        /** 本文の最大文字数 (Dialog の入力欄の上限にも使う) */
        const val MAX_CONTENT_LENGTH: Int = 1000

        /**
         * 本文を検証する (外部状態に依存しない純粋関数)
         *
         * @param content 前後の空白を取り除いた本文
         * @return 不正な場合はその理由、問題なければ null
         */
        fun validateContent(content: String): TicketSubmitResult? = when {
            content.isBlank() -> TicketSubmitResult.BlankContent
            // Discord の Embed や DB 容量を考慮して上限を設ける
            content.length > MAX_CONTENT_LENGTH -> TicketSubmitResult.ContentTooLong(MAX_CONTENT_LENGTH)
            else -> null
        }
    }
}
