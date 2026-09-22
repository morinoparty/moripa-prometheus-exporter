/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.http

import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.morino.prometheusexporter.common.model.config.ExporterConfig

/**
 * Prometheus がスクレイプするメトリクスを公開する HTTP サーバー
 *
 * 設定の host / port / path でクライアントライブラリの [HTTPServer] を起動する。
 * ヘルスチェック用の /-/healthy はライブラリが自動で提供する。
 */
class MetricsHttpServer : KoinComponent {
    private val config: ExporterConfig by inject()
    private val registry: PrometheusRegistry by inject()

    /** 起動中の HTTP サーバー (停止中は null) */
    private var httpServer: HTTPServer? = null

    /** 実際にバインドされたポート番号 (設定で 0 を指定した場合に自動選択された値を知るために使う)。停止中は null */
    val port: Int?
        get() = httpServer?.port

    /**
     * HTTP サーバーを起動する
     *
     * 既に起動している場合は何もしない。
     *
     * @throws java.io.IOException ポートのバインドに失敗した場合 (ログ出力は呼び出し側で行う)
     */
    fun start() {
        // 二重起動を防ぐ
        if (httpServer != null) {
            return
        }
        val serverConfig = config.server
        httpServer = HTTPServer.builder()
            .hostname(serverConfig.host)
            .port(serverConfig.port)
            .registry(registry)
            .metricsHandlerPath(serverConfig.path)
            .buildAndStart()
    }

    /**
     * HTTP サーバーを停止する
     *
     * 起動していない場合は何もしない。
     */
    fun stop() {
        httpServer?.close()
        httpServer = null
    }
}
