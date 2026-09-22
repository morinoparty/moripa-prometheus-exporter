/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.velocity

import com.google.inject.Inject
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.getOrNull
import org.koin.core.context.GlobalContext.loadKoinModules
import org.slf4j.Logger
import party.morino.prometheusexporter.common.BuildConstants
import party.morino.prometheusexporter.common.MoripaPrometheusExporterCommon
import party.morino.prometheusexporter.common.config.ExporterConfigLoader
import party.morino.prometheusexporter.common.di.CommonModule
import party.morino.prometheusexporter.common.http.MetricsHttpServer
import party.morino.prometheusexporter.common.metrics.JvmMetricsCollector
import party.morino.prometheusexporter.common.metrics.MetricsCollector
import party.morino.prometheusexporter.common.metrics.MetricsExporter
import party.morino.prometheusexporter.common.model.config.ExporterConfig
import party.morino.prometheusexporter.common.model.config.HttpServerConfig
import party.morino.prometheusexporter.velocity.di.VelocityModule
import party.morino.prometheusexporter.velocity.metrics.ConnectionEventListener
import party.morino.prometheusexporter.velocity.metrics.ProxyInfoCollector
import party.morino.prometheusexporter.velocity.metrics.ProxyPlayerMetricsCollector
import java.io.IOException
import java.nio.file.Path

/**
 * Velocity プロキシのメトリクスを Prometheus 形式で公開するプラグイン本体
 *
 * コンストラクタ引数は Velocity (Guice) が注入する。ここ以外ではコンストラクタインジェクションを使わず、
 * 自前のクラスは Koin の `by inject()` / `get()` で依存を取得する。
 */
@Plugin(
    id = "moripa-prometheus-exporter",
    name = "MoripaPrometheusExporter",
    // バージョンは Gradle が生成する BuildConstants から取得する (gradle.properties の version と連動)
    version = BuildConstants.VERSION,
    description = "Exports proxy metrics (player count, etc.) for Prometheus / Grafana",
    authors = ["morinoparty"],
)
class MoripaPrometheusExporter @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    // config.json を配置するプラグイン専用のデータフォルダ (plugins/moripa-prometheus-exporter)
    @DataDirectory private val dataDirectory: Path,
) : KoinComponent {

    @Subscribe
    @Suppress("UnusedParameter")
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // Paper (9225) と同じホストで動かしても衝突しないよう、Velocity の既定ポートは 9226 にする
        val config = ExporterConfigLoader(dataDirectory, ExporterConfig(server = HttpServerConfig(port = 9226))).load()
        setupKoin(config)
        MoripaPrometheusExporterCommon.init()

        val commandManager = VelocityCommandManager<CommandSource>(
            server.pluginManager.ensurePluginContainer(this),
            server,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.identity(),
        )

        // TODO: エクスポーター用のコマンド（設定リロードなど）はここで commandManager に登録する

        startMetricsExporter(config)

        logger.info("MoripaPrometheusExporter has been enabled!")
    }

    @Subscribe
    @Suppress("UnusedParameter")
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        // 設定の読み込みに失敗して Koin が起動していない場合もあるので、存在する場合だけ停止する
        getOrNull()?.get<MetricsExporter>()?.stop()
        logger.info("MoripaPrometheusExporter has been disabled!")
    }

    /**
     * コレクターを組み立ててイベントリスナーを登録し、メトリクスの HTTP サーバーを起動する
     *
     * ポートのバインドに失敗してもプロキシ自体の動作には影響しないため、エラーログを出すだけで続行する。
     *
     * @param config 読み込み済みのエクスポーター設定
     */
    private fun startMetricsExporter(config: ExporterConfig) {
        val connectionEventListener = ConnectionEventListener()
        val collectors = buildList<MetricsCollector> {
            // JVM メトリクスは設定で無効化できる
            if (config.metrics.jvm) {
                add(JvmMetricsCollector())
            }
            add(ProxyPlayerMetricsCollector())
            add(ProxyInfoCollector())
            add(connectionEventListener)
        }

        // @Subscribe を持つコレクターは Velocity のイベントリスナーとしても登録する
        server.eventManager.register(this, connectionEventListener)

        try {
            get<MetricsExporter>().start(collectors)
        } catch (e: IOException) {
            logger.error(
                "Failed to bind metrics HTTP server on ${config.server.host}:${config.server.port} - is the port in use?",
                e,
            )
            return
        }

        // ポートに 0 を指定した場合に備えて、実際にバインドされたポートを表示する
        val port = get<MetricsHttpServer>().port ?: config.server.port
        logger.info("Metrics are exposed at http://${config.server.host}:$port${config.server.path}")
    }

    /**
     * Koin DI コンテナの初期化
     *
     * テスト環境などで既に Koin が起動している場合は、既存のコンテナにモジュールを追加する。
     *
     * @param config 読み込み済みのエクスポーター設定
     */
    private fun setupKoin(config: ExporterConfig) {
        val modules = listOf(
            CommonModule.create(config),
            VelocityModule.create(this, server, logger),
        )

        if (getOrNull() != null) {
            loadKoinModules(modules)
            return
        }

        GlobalContext.startKoin {
            modules(modules)
        }
    }
}
