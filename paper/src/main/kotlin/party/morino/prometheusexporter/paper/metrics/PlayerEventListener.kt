/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper.metrics

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import party.morino.prometheusexporter.common.metrics.MetricsCollector

/**
 * プレイヤーの参加 / 退出回数をカウントするリスナー
 */
class PlayerEventListener :
    MetricsCollector,
    Listener {
    override val id: String = "player_events"

    /** プレイヤーの参加回数 */
    private val joins: Counter = Counter.builder()
        .name("minecraft_player_joins_total")
        .help("Total number of player joins")
        .build()

    /** プレイヤーの退出回数 (reason ラベルで切断理由を区別する) */
    private val quits: Counter = Counter.builder()
        .name("minecraft_player_quits_total")
        .help("Total number of player quits by reason (disconnected, kicked, timed_out, erroneous_state)")
        .labelNames("reason")
        .build()

    override fun register(registry: PrometheusRegistry) {
        registry.register(joins)
        registry.register(quits)
    }

    /**
     * プレイヤー参加時にカウントする
     *
     * @param event 参加イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("UnusedParameter")
    fun onPlayerJoin(event: PlayerJoinEvent) {
        joins.inc()
    }

    /**
     * プレイヤー退出時に理由ごとにカウントする
     *
     * @param event 退出イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // ラベル値は Prometheus の慣習に合わせて小文字にする (例: DISCONNECTED -> disconnected)
        quits.labelValues(event.reason.name.lowercase()).inc()
    }
}
