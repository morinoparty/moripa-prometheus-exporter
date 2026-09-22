/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper.metrics

import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.morino.prometheusexporter.common.metrics.SampledMetricsCollector
import party.morino.prometheusexporter.common.model.config.ExporterConfig
import party.morino.prometheusexporter.common.model.config.MetricsConfig
import party.morino.prometheusexporter.paper.MoripaPrometheusExporter

/**
 * サーバーのメインスレッド上で [SampledMetricsCollector] を定期的に実行するサンプラー
 *
 * Bukkit API はメインスレッド以外から触れないため、スクレイプ時ではなく
 * MCCoroutine のコルーチン (Minecraft ディスパッチャ) で一定間隔ごとに値を読み取って Gauge に書き込む。
 * 収集間隔は設定の samplingIntervalTicks (1 tick = 50 ms) で決まる。
 */
class MetricsSampler : KoinComponent {
    private val plugin: MoripaPrometheusExporter by inject()
    private val config: ExporterConfig by inject()

    /** サンプリング対象のコレクター一覧 */
    private var collectors: List<SampledMetricsCollector> = emptyList()

    /** 定期実行中のコルーチン (停止中は null) */
    private var job: Job? = null

    /**
     * サンプリングを開始する
     *
     * 最初の 1 回は同期的に実行し、その後は設定された間隔で繰り返す。
     * 既に開始している場合は何もしない。
     *
     * @param collectors サンプリング対象のコレクター一覧
     */
    fun start(collectors: List<SampledMetricsCollector>) {
        // 二重起動を防ぐ
        if (job != null) {
            return
        }
        this.collectors = collectors
        // 起動直後からメトリクスが揃うように、初回は即座にサンプリングする
        sampleAll()

        // tick 数をミリ秒に変換する (MCCoroutine の ticks 拡張は Int 型なので Long のままミリ秒で扱う)
        // 値が 1 以上かつオーバーフローしない範囲であることは MetricsConfig の init ブロックで検証済み
        val intervalMillis = config.metrics.samplingIntervalTicks * MetricsConfig.MILLIS_PER_TICK
        job = plugin.launch {
            while (isActive) {
                // 初回は start() で同期的に実行済みなので、先に待ってからサンプリングする
                delay(intervalMillis)
                sampleAll()
            }
        }
    }

    /**
     * 登録済みのすべてのコレクターを 1 回サンプリングする
     *
     * 1 つのコレクターが失敗しても他のコレクターに影響しないよう、コレクターごとに例外を捕捉して警告ログに残す。
     * メインスレッドから呼び出すこと (テストでは直接呼び出してもよい)。
     */
    fun sampleAll() {
        collectors.forEach { collector ->
            runCatching { collector.sample() }
                .onFailure { e ->
                    plugin.logger.warning("Failed to sample metrics collector '${collector.id}': $e")
                }
        }
    }

    /**
     * 定期実行を停止する
     *
     * 開始していない場合は何もしない。
     */
    fun stop() {
        job?.cancel()
        job = null
    }
}
