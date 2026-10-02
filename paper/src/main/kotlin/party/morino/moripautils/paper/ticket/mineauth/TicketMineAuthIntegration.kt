/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.paper.ticket.mineauth

import org.koin.core.component.inject
import party.morino.mineauth.api.EndpointRegistrationException
import party.morino.mineauth.api.MineAuthApi
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import party.morino.moripautils.paper.MoripaUtils

/**
 * ticket 機能の HTTP API を MineAuth に登録する連携処理
 *
 * MineAuth は任意依存のため、MineAuth の API クラスに触れるコードはこのクラス (と [TicketApiHandler]) に閉じ込める。
 * 呼び出し側は `server.pluginManager.getPlugin("MineAuth") != null` を確認してからこのクラスを使うこと
 * (確認前に MineAuthApi を参照すると、MineAuth が無い環境で NoClassDefFoundError になる)。
 */
class TicketMineAuthIntegration : MoripaUtilsKoinComponent {
    private val plugin: MoripaUtils by inject()

    /**
     * MineAuth にエンドポイントを登録する
     *
     * 登録の解除は MineAuth がプラグインの無効化時に自動で行う。
     * 登録に失敗しても ticket 機能の他の部分は動かしたいため、例外は投げずにログへ残す。
     */
    fun register() {
        // MineAuth はロード済みだがサービス登録がまだ、という狭いタイミングでは null になる
        val api = MineAuthApi.get(plugin.server)
        if (api == null) {
            plugin.logger.warning("MineAuth API is not available yet; ticket HTTP endpoints are disabled")
            return
        }
        try {
            val registration = api.register(plugin, NAMESPACE, TicketApiHandler())
            plugin.logger.info("Ticket HTTP endpoints are mounted at ${registration.basePath}")
        } catch (e: EndpointRegistrationException) {
            plugin.logger.severe("Failed to register ticket HTTP endpoints to MineAuth: ${e.message}")
        }
    }

    companion object {
        /**
         * MineAuth 上の URL 名前空間 (/api/v1/plugins/moripautils)
         *
         * MineAuth 同梱の PureTickets アドオンが "tickets" を使用しており、同名だと後から登録した側が失敗するため、
         * このプラグイン固有の名前にする。
         */
        const val NAMESPACE: String = "moripautils"
    }
}
