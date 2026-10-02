/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.velocity.observability.metrics

import com.velocitypowered.api.proxy.ProxyServer
import io.prometheus.metrics.core.metrics.Info
import io.prometheus.metrics.model.registry.PrometheusRegistry
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import org.koin.core.component.inject
import party.morino.moripautils.common.model.config.MoripaUtilsConfig
import party.morino.moripautils.common.observability.metrics.MetricsCollector

/**
 * プロキシ自体のバージョン情報を Info メトリクスとして公開するコレクター
 *
 * バインドアドレスは機密性が高いため公開しない。
 */
class ProxyInfoCollector :
    MetricsCollector,
    MoripaUtilsKoinComponent {
    private val server: ProxyServer by inject()
    private val config: MoripaUtilsConfig by inject()

    override val id: String = "proxy_info"

    /** サーバー識別子 (設定の server) / プロキシの実装名 / ベンダー / バージョンをラベルに持つ Info メトリクス */
    private val proxyInfo: Info = Info.builder()
        .name("velocity_proxy_info")
        .help("Proxy identifier, implementation name, vendor and version")
        // server は config.conf の server。複数プロキシを 1 つの Grafana で見分けるために使う
        .labelNames("server", "name", "vendor", "version")
        .build()

    override fun register(registry: PrometheusRegistry) {
        // バージョン情報は起動中に変わらないので、登録時に一度だけラベル値を設定する
        val version = server.version
        proxyInfo.setLabelValues(config.server, version.name, version.vendor, version.version)
        registry.register(proxyInfo)
    }
}
