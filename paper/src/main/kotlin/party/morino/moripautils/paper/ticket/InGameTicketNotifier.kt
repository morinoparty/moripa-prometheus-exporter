/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.paper.ticket

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Server
import org.koin.core.component.inject
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import party.morino.moripautils.common.model.config.TicketCategory
import party.morino.moripautils.common.model.ticket.Ticket
import party.morino.moripautils.common.ticket.TicketNotifier
import party.morino.moripautils.paper.MoripaUtils
import java.util.logging.Level

/**
 * 新しいチケットを、通知権限 (moripautils.ticket.notify) を持つオンラインのプレイヤーへチャットで知らせる通知先
 */
class InGameTicketNotifier :
    TicketNotifier,
    MoripaUtilsKoinComponent {
    private val plugin: MoripaUtils by inject()
    private val server: Server by inject()

    override suspend fun notify(ticket: Ticket, category: TicketCategory) {
        try {
            // オンラインプレイヤーの一覧や権限はメインスレッドで読む
            withContext(plugin.minecraftDispatcher) {
                server.onlinePlayers
                    .filter { it.hasPermission(TicketPermissions.NOTIFY) }
                    .forEach { staff ->
                        // プレイヤー名やカテゴリー名に MiniMessage のタグが含まれていても解釈させない
                        staff.sendRichMessage(
                            "<gold>[Ticket]</gold> <player> さんから新しいお問い合わせがあります " +
                                "<gray>(#<id> / <category> / <server>)",
                            Placeholder.unparsed("player", ticket.playerName),
                            Placeholder.unparsed("id", ticket.id.toString()),
                            Placeholder.unparsed("category", category.name),
                            Placeholder.unparsed("server", ticket.serverId),
                        )
                    }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 通知の失敗でチケットの送信を失敗させない
            plugin.logger.log(Level.WARNING, "Failed to notify staff of ticket #${ticket.id}", e)
        }
    }
}
