/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.model.config

import kotlinx.serialization.Serializable

/**
 * お問い合わせ (ticket 機能) の設定
 *
 * @property enabled ticket 機能を有効にするかどうか。false の場合は /ticket コマンドやデータベースを用意しない
 * @property categories プレイヤーが選択できるお問い合わせのカテゴリー (表示順)
 * @property webhook 新しいチケットを外部へ通知する Webhook の設定
 * @property database チケットを保存するデータベースの設定
 * @throws IllegalArgumentException categories が空、または id が重複している場合
 */
@Serializable
data class TicketConfig(
    val enabled: Boolean = true,
    val categories: List<TicketCategory> = DEFAULT_CATEGORIES,
    val webhook: TicketWebhookConfig = TicketWebhookConfig(),
    val database: TicketDatabaseConfig = TicketDatabaseConfig(),
) {
    init {
        // カテゴリーが 1 つもないとプレイヤーが送信できないため、設定の読み込み時に失敗させる
        require(categories.isNotEmpty()) { "ticket.categories must not be empty" }
        // id はデータベースに保存してカテゴリーを引き当てるキーなので重複を許さない
        val duplicatedIds = categories.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicatedIds.isEmpty()) { "ticket.categories ids must be unique, but duplicated: $duplicatedIds" }
    }

    companion object {
        /** 既定のカテゴリー一覧 */
        val DEFAULT_CATEGORIES: List<TicketCategory> = listOf(
            TicketCategory(id = "bug", name = "バグの報告"),
            TicketCategory(id = "protect", name = "土地保護について"),
            TicketCategory(id = "other", name = "その他"),
        )
    }
}
