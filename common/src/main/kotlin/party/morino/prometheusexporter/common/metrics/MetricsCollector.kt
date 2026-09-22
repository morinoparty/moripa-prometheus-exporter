/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.metrics

import io.prometheus.metrics.model.registry.PrometheusRegistry

/**
 * メトリクスを Prometheus のレジストリに登録するコレクターの共通インターフェース
 *
 * 実装クラスはメトリクスファミリー (Gauge / Counter / Histogram など) を [register] で一度だけ登録する。
 * [register] はコレクターごとに正確に一度だけ呼ばれる。
 */
interface MetricsCollector {
    /** コレクターを識別する ID (ログや登録失敗時のエラーメッセージに使う) */
    val id: String

    /**
     * メトリクスファミリーをレジストリに登録する
     *
     * @param registry 登録先のレジストリ
     * @throws IllegalArgumentException 同名のメトリクスが既に登録されている場合
     */
    fun register(registry: PrometheusRegistry)
}
