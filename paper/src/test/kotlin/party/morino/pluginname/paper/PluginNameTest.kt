/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.pluginname.paper

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.getOrNull
import org.koin.dsl.module
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

/**
 * テスト用の JUnit 5 拡張クラス
 * MockBukkit と Koin の初期化・クリーンアップを行う
 */
class PluginNameTest :
    BeforeAllCallback,
    AfterAllCallback {

    companion object {
        lateinit var server: ServerMock
        lateinit var plugin: PluginName
    }

    override fun beforeAll(context: ExtensionContext) {
        // MockBukkit サーバーを初期化
        server = MockBukkit.mock()

        // テスト用の Koin モジュールを設定
        val appModule = module {
            single<ServerMock> { server }
        }

        // Koin が未初期化の場合のみ開始
        getOrNull() ?: GlobalContext.startKoin {
            modules(appModule)
        }

        // プラグインをロード
        plugin = MockBukkit.load(PluginName::class.java)

        // プラグインインスタンスを Koin に登録
        GlobalContext.get().declare(plugin)
    }

    override fun afterAll(context: ExtensionContext) {
        MockBukkit.unmock()
        GlobalContext.stopKoin()
    }
}
