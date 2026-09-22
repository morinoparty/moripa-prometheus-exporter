/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper.metrics

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Histogram
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.Unit as PrometheusUnit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import party.morino.prometheusexporter.common.metrics.MetricsCollector

/**
 * 毎 tick の処理時間をヒストグラムとして記録するリスナー
 *
 * [ServerTickEndEvent] は毎 tick メインスレッドで発火するため、ハンドラは値の記録だけを行う。
 */
class TickDurationListener :
    MetricsCollector,
    Listener {
    override val id: String = "tick_duration"

    /** tick 処理時間の分布 (秒)。バケットは 5 ms (1 tick の 1/10) から 5 秒まで */
    private val tickDuration: Histogram = Histogram.builder()
        .name("minecraft_tick_duration_seconds")
        .help("Distribution of server tick durations in seconds")
        .unit(PrometheusUnit.SECONDS)
        // ネイティブヒストグラムは使わず、Grafana で扱いやすい固定バケットのみにする
        .classicOnly()
        .classicUpperBounds(0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0)
        .build()

    /** 処理した tick の総数 */
    private val ticks: Counter = Counter.builder()
        .name("minecraft_ticks_total")
        .help("Total number of server ticks processed")
        .build()

    override fun register(registry: PrometheusRegistry) {
        registry.register(tickDuration)
        registry.register(ticks)
    }

    /**
     * tick 終了時に処理時間を記録する
     *
     * @param event tick 終了イベント (tickDuration はミリ秒)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onTickEnd(event: ServerTickEndEvent) {
        // getTickDuration はミリ秒 (double) なので秒に変換する
        tickDuration.observe(event.tickDuration / MILLIS_PER_SECOND)
        ticks.inc()
    }

    companion object {
        /** ミリ秒から秒への換算係数 */
        private const val MILLIS_PER_SECOND = 1000.0
    }
}
