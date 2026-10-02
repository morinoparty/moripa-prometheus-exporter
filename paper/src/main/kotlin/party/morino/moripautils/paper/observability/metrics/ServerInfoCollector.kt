/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.paper.observability.metrics

import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.core.metrics.Info
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.bukkit.Server
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import org.koin.core.component.inject
import party.morino.moripautils.common.model.config.MoripaUtilsConfig
import party.morino.moripautils.common.observability.metrics.SampledMetricsCollector

/**
 * サーバーのバージョン情報とプラグイン数を公開するコレクター
 *
 * バージョン情報は起動後に変わらないため [register] で一度だけ設定し、プラグイン数は定期的にサンプリングする。
 */
class ServerInfoCollector :
    SampledMetricsCollector,
    MoripaUtilsKoinComponent {
    private val server: Server by inject()
    private val config: MoripaUtilsConfig by inject()

    override val id: String = "server_info"

    /** サーバー識別子 (設定の server) / サーバー実装 / Bukkit API / Minecraft のバージョン (値は常に 1) */
    private val serverInfo: Info = Info.builder()
        .name("minecraft_server_info")
        .help("Server identifier, implementation, Bukkit API and Minecraft versions")
        // server は config.conf の server。複数サーバーを 1 つの Grafana で見分けるために使う
        .labelNames("server", "version", "bukkit_version", "minecraft_version")
        .build()

    /** ロードされているプラグイン数 (無効化されたものも含む) */
    private val pluginsLoaded: Gauge = Gauge.builder()
        .name("minecraft_plugins_loaded")
        .help("Number of plugins loaded on the server")
        .build()

    /** 有効化されているプラグイン数 */
    private val pluginsEnabled: Gauge = Gauge.builder()
        .name("minecraft_plugins_enabled")
        .help("Number of plugins currently enabled")
        .build()

    override fun register(registry: PrometheusRegistry) {
        registry.register(serverInfo)
        registry.register(pluginsLoaded)
        registry.register(pluginsEnabled)
        // バージョンは起動中に変わらないので登録時に一度だけ設定する
        serverInfo.setLabelValues(config.server, server.version, server.bukkitVersion, server.minecraftVersion)
    }

    override fun sample() {
        val plugins = server.pluginManager.plugins
        pluginsLoaded.set(plugins.size.toDouble())
        pluginsEnabled.set(plugins.count { it.isEnabled }.toDouble())
    }
}
