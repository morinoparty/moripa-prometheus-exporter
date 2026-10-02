/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.paper

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

/**
 * テスト用の JUnit 5 拡張クラス
 * MockBukkit の初期化・クリーンアップを行う
 *
 * Koin コンテナはプラグインの有効化時に MoripaUtilsKoinContext へ専用に作られ、
 * 無効化時 (MockBukkit.unmock) に閉じられるため、ここでは GlobalContext を扱わない。
 * テストクラスは MoripaUtilsKoinComponent を実装すれば `by inject()` でプラグインの依存を取得できる。
 */
class MoripaUtilsTest :
    BeforeAllCallback,
    AfterAllCallback {

    companion object {
        lateinit var server: ServerMock
        lateinit var plugin: MoripaUtils
    }

    override fun beforeAll(context: ExtensionContext) {
        // MockBukkit サーバーを初期化
        server = MockBukkit.mock()
        // プラグインをロード (ブートストラッパーを経由しないため、コマンドマネージャーは null になる)
        plugin = MockBukkit.load(MoripaUtils::class.java)
    }

    override fun afterAll(context: ExtensionContext) {
        // プラグインが無効化され、専用の Koin コンテナも閉じられる
        MockBukkit.unmock()
    }
}
