/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.velocity.metrics

import com.velocitypowered.api.proxy.ProxyServer
import io.prometheus.metrics.core.metrics.GaugeWithCallback
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.morino.prometheusexporter.common.metrics.MetricsCollector

/**
 * プロキシのプレイヤー数 / バックエンドサーバー / プラグイン数を公開するコレクター
 *
 * Velocity には Paper のような tick スレッドがなく、ProxyServer の読み取りは軽量なメモリアクセスなので、
 * サンプリングではなくスクレイプ時に値を読み取る [GaugeWithCallback] を使う。
 */
class ProxyPlayerMetricsCollector :
    MetricsCollector,
    KoinComponent {
    private val server: ProxyServer by inject()

    override val id: String = "proxy_players"

    /** プロキシに接続中のプレイヤー数 */
    private val playersOnline: GaugeWithCallback = GaugeWithCallback.builder()
        .name("velocity_players_online")
        .help("Number of players currently connected to the proxy")
        .callback { callback -> callback.call(server.playerCount.toDouble()) }
        .build()

    /** サーバーリストに表示する最大プレイヤー数 (velocity.toml の show-max-players) */
    private val showMaxPlayers: GaugeWithCallback = GaugeWithCallback.builder()
        .name("velocity_show_max_players")
        .help("show-max-players from velocity.toml, not a hard cap")
        .callback { callback -> callback.call(server.configuration.showMaxPlayers.toDouble()) }
        .build()

    /** バックエンドサーバーごとの接続プレイヤー数 */
    private val backendPlayers: GaugeWithCallback = GaugeWithCallback.builder()
        .name("velocity_backend_players")
        .help("Number of players connected to each backend server")
        .labelNames("server")
        .callback { callback ->
            // 登録済みのバックエンドサーバーごとに、サーバー名をラベルにして接続数を報告する
            server.allServers.forEach { registeredServer ->
                callback.call(registeredServer.playersConnected.size.toDouble(), registeredServer.serverInfo.name)
            }
        }
        .build()

    /** プロキシに登録されているバックエンドサーバーの数 */
    private val backendServers: GaugeWithCallback = GaugeWithCallback.builder()
        .name("velocity_backend_servers")
        .help("Number of backend servers registered on the proxy")
        .callback { callback -> callback.call(server.allServers.size.toDouble()) }
        .build()

    /** プロキシに読み込まれているプラグインの数 */
    private val pluginsLoaded: GaugeWithCallback = GaugeWithCallback.builder()
        .name("velocity_plugins_loaded")
        .help("Number of plugins loaded on the proxy")
        .callback { callback -> callback.call(server.pluginManager.plugins.size.toDouble()) }
        .build()

    override fun register(registry: PrometheusRegistry) {
        // コールバック型の Gauge はスクレイプ時に値を読むため、レジストリに登録するだけでよい
        registry.register(playersOnline)
        registry.register(showMaxPlayers)
        registry.register(backendPlayers)
        registry.register(backendServers)
        registry.register(pluginsLoaded)
    }
}
