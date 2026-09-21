/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.pluginname.paper

import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import party.morino.pluginname.paper.commands.ExampleCommand

@Suppress("unused", "UnstableApiUsage")
class PluginNameBootstrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        val commandManager: PaperCommandManager<CommandSourceStack> =
            PaperCommandManager
                .builder()
                .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
                .buildBootstrapped(context)

        ExampleCommand(commandManager).register()
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin {
        return PluginName()
    }
}
