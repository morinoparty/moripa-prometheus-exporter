/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.model.config

import kotlinx.serialization.Serializable

/**
 * 収集するメトリクスに関する設定
 *
 * @property jvm JVM のランタイム情報 (メモリ / GC / スレッドなど) を公開するかどうか
 * @property samplingIntervalTicks メインスレッドでサンプリングするメトリクスの収集間隔 (tick 単位)。
 *   Paper 専用の設定で、20 tick = 1 秒。Velocity には tick の概念がないため、
 *   サンプラーが必要になった場合は samplingIntervalTicks * 50 ミリ秒として解釈する。
 *   1 以上でなければならず、0 以下を指定すると設定の読み込み時に失敗する
 * @throws IllegalArgumentException samplingIntervalTicks が 1 未満、またはミリ秒換算でオーバーフローする値の場合
 */
@Serializable
data class MetricsConfig(
    val jvm: Boolean = true,
    val samplingIntervalTicks: Long = 20,
) {
    init {
        // 0 以下では delay() が中断せずメインスレッドを占有してサーバーが固まるため、
        // またミリ秒換算 (50 倍) でオーバーフローしない範囲に限定するため、ここで早期に失敗させる。
        // kotlinx.serialization はデシリアライズ時にも init ブロックを実行するので、config.json の値も検証される
        require(samplingIntervalTicks in 1..(Long.MAX_VALUE / MILLIS_PER_TICK)) {
            "metrics.samplingIntervalTicks must be a positive tick count, but was $samplingIntervalTicks"
        }
    }

    companion object {
        /** 1 tick あたりのミリ秒数 (Minecraft サーバーは 20 tick/秒で動作する) */
        const val MILLIS_PER_TICK: Long = 50L
    }
}
