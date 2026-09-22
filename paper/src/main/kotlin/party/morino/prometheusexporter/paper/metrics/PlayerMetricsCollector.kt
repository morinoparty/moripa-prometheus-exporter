/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper.metrics

import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.bukkit.Server
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.morino.prometheusexporter.common.metrics.SampledMetricsCollector

/**
 * オンラインプレイヤー数と最大プレイヤー数を公開するコレクター
 */
class PlayerMetricsCollector :
    SampledMetricsCollector,
    KoinComponent {
    private val server: Server by inject()

    override val id: String = "players"

    /** 現在オンラインのプレイヤー数 */
    private val playersOnline: Gauge = Gauge.builder()
        .name("minecraft_players_online")
        .help("Number of players currently online")
        .build()

    /** server.properties の max-players */
    private val playersMax: Gauge = Gauge.builder()
        .name("minecraft_players_max")
        .help("Maximum number of players allowed on the server")
        .build()

    override fun register(registry: PrometheusRegistry) {
        registry.register(playersOnline)
        registry.register(playersMax)
    }

    override fun sample() {
        playersOnline.set(server.onlinePlayers.size.toDouble())
        playersMax.set(server.maxPlayers.toDouble())
    }
}
