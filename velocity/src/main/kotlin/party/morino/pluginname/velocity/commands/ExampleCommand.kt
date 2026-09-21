/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.pluginname.velocity.commands

import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.text.Component
import org.incendo.cloud.velocity.VelocityCommandManager

class ExampleCommand(
    private val commandManager: VelocityCommandManager<CommandSource>,
) {
    fun register() {
        val command = commandManager.commandBuilder("pluginname")
            .handler { ctx ->
                ctx.sender().sendMessage(Component.text("Hello from PluginName!"))
            }
            .build()

        commandManager.command(command)
    }
}
