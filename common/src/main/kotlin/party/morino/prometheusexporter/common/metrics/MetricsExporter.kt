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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.morino.prometheusexporter.common.http.MetricsHttpServer

/**
 * コレクターの登録と HTTP サーバーの起動 / 停止をまとめて行うエクスポーターの入口
 *
 * プラットフォーム側のプラグインは、収集したいコレクターの一覧を [start] に渡すだけでよい。
 */
class MetricsExporter : KoinComponent {
    private val registry: PrometheusRegistry by inject()
    private val httpServer: MetricsHttpServer by inject()

    /** 起動済みかどうか (二重 start / stop を無視するためのガード) */
    private var isStarted: Boolean = false

    /**
     * コレクターをレジストリに登録し、HTTP サーバーを起動する
     *
     * 既に起動済みの場合は何もしない。
     *
     * @param collectors 登録するコレクターの一覧
     * @throws IllegalArgumentException 同名のメトリクスが重複して登録された場合 (どのコレクターかをメッセージに含める)
     * @throws java.io.IOException HTTP サーバーのバインドに失敗した場合
     */
    fun start(collectors: List<MetricsCollector>) {
        if (isStarted) {
            return
        }
        collectors.forEach { collector ->
            try {
                collector.register(registry)
            } catch (e: IllegalArgumentException) {
                // どのコレクターで重複登録が起きたのか分かるように ID を付けて再スローする
                throw IllegalArgumentException("Failed to register metrics collector '${collector.id}': ${e.message}", e)
            }
        }
        httpServer.start()
        // バインドに成功してから起動済みにする (失敗時は stop() が no-op のままでよい)
        isStarted = true
    }

    /**
     * HTTP サーバーを停止する
     *
     * 起動していない場合は何もしない。
     */
    fun stop() {
        if (!isStarted) {
            return
        }
        httpServer.stop()
        isStarted = false
    }
}
