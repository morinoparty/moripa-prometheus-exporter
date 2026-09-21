/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.pluginname.paper

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.getOrNull
import org.koin.dsl.module
import party.morino.pluginname.common.PluginNameCommon

open class PluginName : SuspendingJavaPlugin() {

    override suspend fun onEnableAsync() {
        setupKoin()
        PluginNameCommon.init()
        logger.info("${pluginMeta.name} v${pluginMeta.version} has been enabled!")
    }

    override suspend fun onDisableAsync() {
        logger.info("${pluginMeta.name} has been disabled!")
    }

    /**
     * Koin DI コンテナの初期化
     * テスト環境では既に初期化済みの場合があるため、チェックを行う
     */
    private fun setupKoin() {
        if (getOrNull() != null) {
            return
        }

        val appModule = module {
            single<PluginName> { this@PluginName }
        }

        getOrNull() ?: GlobalContext.startKoin {
            modules(appModule)
        }
    }
}
