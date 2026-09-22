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
import io.prometheus.metrics.model.snapshots.Unit as PrometheusUnit
import org.bukkit.Server
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.morino.prometheusexporter.common.metrics.SampledMetricsCollector

/**
 * TPS / 平均 tick 時間 / tick レートの状態を公開するコレクター
 *
 * 値はすべて Paper の Server API から読み取るため、メインスレッド上でサンプリングする。
 */
class TickMetricsCollector :
    SampledMetricsCollector,
    KoinComponent {
    private val server: Server by inject()

    override val id: String = "tick"

    /** 直近 1 分 / 5 分 / 15 分の平均 TPS (period ラベルで区別する) */
    private val tps: Gauge = Gauge.builder()
        .name("minecraft_tps")
        .help("Average ticks per second over the last 1m, 5m and 15m")
        .labelNames("period")
        .build()

    /** 直近の平均 tick 処理時間 (秒) */
    private val tickDurationAverage: Gauge = Gauge.builder()
        .name("minecraft_tick_duration_average_seconds")
        .help("Average tick duration in seconds")
        .unit(PrometheusUnit.SECONDS)
        .build()

    /** サーバーが目標としている tick レート (/tick rate で変更できる) */
    private val tickRateTarget: Gauge = Gauge.builder()
        .name("minecraft_tick_rate_target")
        .help("Target tick rate of the server (ticks per second)")
        .build()

    /** tick が凍結されているかどうか (1 = 凍結中) */
    private val tickFrozen: Gauge = Gauge.builder()
        .name("minecraft_tick_frozen")
        .help("Whether the server tick is frozen (1) or not (0)")
        .build()

    /** tick スプリント中かどうか (1 = スプリント中) */
    private val tickSprinting: Gauge = Gauge.builder()
        .name("minecraft_tick_sprinting")
        .help("Whether the server is sprinting ticks (1) or not (0)")
        .build()

    override fun register(registry: PrometheusRegistry) {
        registry.register(tps)
        registry.register(tickDurationAverage)
        registry.register(tickRateTarget)
        registry.register(tickFrozen)
        registry.register(tickSprinting)
    }

    override fun sample() {
        // Server#getTPS は [1m, 5m, 15m] の順に並んだ配列を返す
        val tpsValues = server.tps
        TPS_PERIODS.forEachIndexed { index, period ->
            if (index < tpsValues.size) {
                tps.labelValues(period).set(tpsValues[index])
            }
        }
        // getAverageTickTime はミリ秒 (double) なので秒に変換する
        tickDurationAverage.set(server.averageTickTime / MILLIS_PER_SECOND)

        val tickManager = server.serverTickManager
        tickRateTarget.set(tickManager.tickRate.toDouble())
        tickFrozen.set(tickManager.isFrozen.toGaugeValue())
        tickSprinting.set(tickManager.isSprinting.toGaugeValue())
    }

    /** Boolean を Gauge 用の 0 / 1 に変換する */
    private fun Boolean.toGaugeValue(): Double = if (this) 1.0 else 0.0

    companion object {
        /** Server#getTPS の配列インデックスに対応する period ラベルの値 */
        private val TPS_PERIODS = listOf("1m", "5m", "15m")

        /** ミリ秒から秒への換算係数 */
        private const val MILLIS_PER_SECOND = 1000.0
    }
}
