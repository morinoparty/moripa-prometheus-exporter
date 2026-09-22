/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.getOrNull
import party.morino.prometheusexporter.common.MoripaPrometheusExporterCommon
import party.morino.prometheusexporter.common.config.ExporterConfigLoader
import party.morino.prometheusexporter.common.di.CommonModule
import party.morino.prometheusexporter.common.http.MetricsHttpServer
import party.morino.prometheusexporter.common.metrics.JvmMetricsCollector
import party.morino.prometheusexporter.common.metrics.MetricsCollector
import party.morino.prometheusexporter.common.metrics.MetricsExporter
import party.morino.prometheusexporter.common.metrics.SampledMetricsCollector
import party.morino.prometheusexporter.common.model.config.ExporterConfig
import party.morino.prometheusexporter.paper.di.PaperModule
import party.morino.prometheusexporter.paper.metrics.ChunkEventListener
import party.morino.prometheusexporter.paper.metrics.MetricsSampler
import party.morino.prometheusexporter.paper.metrics.PlayerEventListener
import party.morino.prometheusexporter.paper.metrics.PlayerMetricsCollector
import party.morino.prometheusexporter.paper.metrics.ServerInfoCollector
import party.morino.prometheusexporter.paper.metrics.TickDurationListener
import party.morino.prometheusexporter.paper.metrics.TickMetricsCollector
import party.morino.prometheusexporter.paper.metrics.WorldMetricsCollector
import java.io.IOException

/**
 * Paper 向けプラグイン本体
 *
 * 有効化時に設定を読み込み、Koin を初期化してメトリクスコレクターを登録し、
 * Prometheus 用の HTTP サーバーとメインスレッドのサンプラーを起動する。
 */
open class MoripaPrometheusExporter :
    SuspendingJavaPlugin(),
    KoinComponent {

    /** このプラグインが Koin を開始したかどうか (テストでは事前に開始されているため停止しない) */
    private var koinStartedByPlugin: Boolean = false

    override suspend fun onEnableAsync() {
        val config = loadConfig()
        setupKoin(config)
        MoripaPrometheusExporterCommon.init()

        val collectors = createCollectors(config)
        // Bukkit のイベントを購読するコレクターはリスナーとして登録する
        collectors.filterIsInstance<Listener>().forEach { listener ->
            server.pluginManager.registerEvents(listener, this)
        }

        startExporter(config, collectors)
        // メインスレッドでしか読めない値は定期的にサンプリングする
        get<MetricsSampler>().start(collectors.filterIsInstance<SampledMetricsCollector>())

        logger.info("${pluginMeta.name} v${pluginMeta.version} has been enabled!")
    }

    override suspend fun onDisableAsync() {
        // 有効化に失敗して Koin やモジュールが存在しない場合もあるため、定義があるときだけ停止処理を行う
        getOrNull()?.let { koin ->
            koin.getOrNull<MetricsSampler>()?.stop()
            koin.getOrNull<MetricsExporter>()?.stop()
        }
        if (koinStartedByPlugin) {
            GlobalContext.stopKoin()
            koinStartedByPlugin = false
        }
        logger.info("${pluginMeta.name} has been disabled!")
    }

    /**
     * データフォルダの config.json を読み込む (存在しなければ既定値を書き出す)
     *
     * @return 読み込んだ設定
     * @throws IllegalStateException config.json の内容が不正な場合 (プラグインの有効化を失敗させる)
     */
    private fun loadConfig(): ExporterConfig {
        val loader = ExporterConfigLoader(dataFolder.toPath(), ExporterConfig())
        return try {
            loader.load()
        } catch (e: IllegalStateException) {
            // 壊れた設定で黙って既定値を使うより、起動を止めて気付いてもらう
            logger.severe("Failed to load config.json: ${e.message}")
            throw e
        }
    }

    /**
     * Koin DI コンテナの初期化
     *
     * テスト環境では既に初期化済みの場合があるため、その場合はモジュールの追加のみ行う。
     *
     * @param config 読み込み済みの設定
     */
    private fun setupKoin(config: ExporterConfig) {
        val modules = listOf(
            CommonModule.create(config),
            PaperModule.create(this),
        )
        if (getOrNull() != null) {
            // 既存のコンテナ (テストが開始したもの) にモジュールを追加する
            GlobalContext.loadKoinModules(modules)
            return
        }
        GlobalContext.startKoin {
            modules(modules)
        }
        koinStartedByPlugin = true
    }

    /**
     * 登録するコレクターの一覧を組み立てる
     *
     * @param config 読み込み済みの設定 (JVM メトリクスの有効 / 無効に使う)
     * @return 登録順に並んだコレクターの一覧
     */
    private fun createCollectors(config: ExporterConfig): List<MetricsCollector> = buildList {
        // JVM メトリクスは設定で無効化できる
        if (config.metrics.jvm) {
            add(JvmMetricsCollector())
        }
        add(ServerInfoCollector())
        add(PlayerMetricsCollector())
        add(TickMetricsCollector())
        add(TickDurationListener())
        add(WorldMetricsCollector())
        add(PlayerEventListener())
        add(ChunkEventListener())
    }

    /**
     * コレクターを登録して HTTP サーバーを起動する
     *
     * ポートのバインドに失敗しても、他のプラグインに影響しないようプラグイン自体は無効化しない。
     *
     * @param config 読み込み済みの設定 (ログ出力用)
     * @param collectors 登録するコレクターの一覧
     */
    private fun startExporter(config: ExporterConfig, collectors: List<MetricsCollector>) {
        val serverConfig = config.server
        try {
            get<MetricsExporter>().start(collectors)
        } catch (e: IOException) {
            logger.severe(
                "Failed to bind metrics HTTP server on ${serverConfig.host}:${serverConfig.port} - is the port in use? (${e.message})",
            )
            return
        }
        // ポートに 0 を指定した場合は自動選択された実際のポートを表示する
        val port = get<MetricsHttpServer>().port ?: serverConfig.port
        logger.info("Metrics are available at http://${serverConfig.host}:$port${serverConfig.path}")
    }
}
