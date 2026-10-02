/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.ticket.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import party.morino.moripautils.common.model.config.TicketConfig
import party.morino.moripautils.common.ticket.TicketNotifier
import party.morino.moripautils.common.ticket.TicketRepository
import party.morino.moripautils.common.ticket.TicketService
import party.morino.moripautils.common.ticket.database.SqliteTicketRepository
import party.morino.moripautils.common.ticket.webhook.TicketWebhookNotifier
import java.nio.file.Path
import java.util.logging.Logger

/**
 * ticket 機能の Koin モジュールを生成するファクトリ
 *
 * Exposed / SQLite に依存するクラスを含むため、ticket 機能が有効な Paper でだけ読み込むこと
 * (Velocity の JAR には Exposed を同梱していない)。
 */
object TicketModule {
    /**
     * ticket 機能の共通部分 (リポジトリ / Webhook 通知 / サービス) のモジュールを生成する
     *
     * @param dataDirectory プラグインのデータフォルダ (データベースファイルの基準になる)
     * @param config ticket 機能の設定
     * @param logger 通知の失敗などを記録するロガー
     * @return リポジトリ / Webhook 通知 / サービスをシングルトンとして提供する Koin モジュール
     */
    fun create(dataDirectory: Path, config: TicketConfig, logger: Logger): Module = module {
        // 相対パスはデータフォルダからの相対として扱う (絶対パスを指定した場合はそのまま使われる)
        single<TicketRepository> { SqliteTicketRepository(dataDirectory.resolve(config.database.file)) }
        // 通知先は TicketNotifier として bind し、TicketService が getAll でまとめて取り出す
        single { TicketWebhookNotifier(logger) } bind TicketNotifier::class
        // 通知は送信結果の返却と切り離して行う。SupervisorJob により 1 件の失敗が他の通知を巻き込まない
        single { TicketService(logger, CoroutineScope(SupervisorJob() + Dispatchers.IO)) }
    }
}
