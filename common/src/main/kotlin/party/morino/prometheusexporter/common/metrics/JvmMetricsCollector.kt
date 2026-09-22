/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.metrics

import io.prometheus.metrics.instrumentation.jvm.JvmBufferPoolMetrics
import io.prometheus.metrics.instrumentation.jvm.JvmClassLoadingMetrics
import io.prometheus.metrics.instrumentation.jvm.JvmCompilationMetrics
import io.prometheus.metrics.instrumentation.jvm.JvmGarbageCollectorMetrics
import io.prometheus.metrics.instrumentation.jvm.JvmMemoryMetrics
import io.prometheus.metrics.instrumentation.jvm.JvmNativeMemoryMetrics
import io.prometheus.metrics.instrumentation.jvm.JvmRuntimeInfoMetric
import io.prometheus.metrics.instrumentation.jvm.JvmThreadsMetrics
import io.prometheus.metrics.instrumentation.jvm.ProcessMetrics
import io.prometheus.metrics.model.registry.PrometheusRegistry

/**
 * JVM のランタイム情報 (メモリ / GC / スレッド / クラス / プロセス情報など) を公開するコレクター
 *
 * Prometheus クライアント標準の jvm_* / process_* メトリクスファミリーを登録する。
 *
 * ライブラリの `JvmMetrics.builder().register(registry)` は登録したレジストリを static な集合に保持し続けるため、
 * プラグインの有効化 / 無効化のたびに新しいレジストリを作る本プラグインでは、古いレジストリが解放されずにリークする。
 * これを避けるため、ファミリーごとのビルダーを直接呼び出して static な状態を残さずに登録する。
 *
 * `JvmMemoryPoolAllocationMetrics` (`jvm_memory_pool_allocated_bytes_total`) は意図的に登録しない。
 * このファミリーは GC の JMX 通知リスナーを登録するが、ライブラリ側にそれを解除する手段がなく、
 * 再有効化のたびにリスナーが蓄積して古いレジストリ (と `/reload` 時には古いクラスローダー) を保持し続けるためである。
 */
class JvmMetricsCollector : MetricsCollector {
    override val id: String = "jvm"

    override fun register(registry: PrometheusRegistry) {
        // JvmMetrics ファサードを経由せず、各ファミリーを直接登録する (理由はクラスの KDoc を参照)
        JvmThreadsMetrics.builder().register(registry)
        JvmBufferPoolMetrics.builder().register(registry)
        JvmClassLoadingMetrics.builder().register(registry)
        JvmCompilationMetrics.builder().register(registry)
        JvmGarbageCollectorMetrics.builder().register(registry)
        JvmMemoryMetrics.builder().register(registry)
        JvmNativeMemoryMetrics.builder().register(registry)
        JvmRuntimeInfoMetric.builder().register(registry)
        ProcessMetrics.builder().register(registry)
    }
}
