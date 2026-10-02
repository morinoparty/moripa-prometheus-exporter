/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.paper.ticket.command

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import party.morino.moripautils.common.di.MoripaUtilsKoinContext
import party.morino.moripautils.paper.MoripaUtils
import party.morino.moripautils.paper.ticket.TicketPermissions
import party.morino.moripautils.paper.ticket.dialog.TicketDialogFactory

/**
 * /ticket コマンド (お問い合わせ用の Dialog を開く)
 *
 * ブートストラップ段階で登録するため、生成時点では Koin コンテナがまだ存在しない。
 * 依存はコマンドの実行時にコンテナから取り出す
 * (by inject() で保持すると、プラグインの再有効化で作り直された古いインスタンスを使い続けてしまう)。
 */
@Suppress("UnstableApiUsage")
class TicketCommand {
    /**
     * お問い合わせ用の Dialog を開く
     *
     * @param source コマンドの実行元
     */
    @Command("ticket")
    @Permission(TicketPermissions.USE)
    @CommandDescription("運営にお問い合わせを送信します")
    suspend fun ticket(source: CommandSourceStack) {
        val sender = source.sender
        // Dialog はプレイヤーのクライアントにしか表示できない
        val player = sender as? Player
        if (player == null) {
            sender.sendRichMessage("<red>このコマンドはプレイヤーのみ実行できます。")
            return
        }
        // プラグインの有効化に失敗した場合などはコンテナや定義が存在しない
        val koin = MoripaUtilsKoinContext.getOrNull()
        val plugin = koin?.getOrNull<MoripaUtils>()
        val dialogFactory = koin?.getOrNull<TicketDialogFactory>()
        if (plugin == null || dialogFactory == null) {
            player.sendRichMessage("<red>現在お問い合わせを受け付けていません。")
            return
        }
        // Cloud の非同期コーディネーターから呼ばれるため、Dialog の表示はメインスレッドで行う
        withContext(plugin.minecraftDispatcher) {
            player.showDialog(dialogFactory.create())
        }
    }
}
