/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.velocity

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
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import org.koin.core.component.get
import org.slf4j.Logger
import party.morino.moripautils.common.BuildConstants
import party.morino.moripautils.common.MoripaUtilsCommon
import party.morino.moripautils.common.config.MoripaUtilsConfigLoader
import party.morino.moripautils.common.di.CommonModule
import party.morino.moripautils.common.di.MoripaUtilsKoinContext
import party.morino.moripautils.common.observability.http.MetricsHttpServer
import party.morino.moripautils.common.observability.metrics.JvmMetricsCollector
import party.morino.moripautils.common.observability.metrics.MetricsCollector
import party.morino.moripautils.common.observability.metrics.MetricsExporter
import party.morino.moripautils.common.model.config.HttpServerConfig
import party.morino.moripautils.common.model.config.MoripaUtilsConfig
import party.morino.moripautils.common.model.config.ObservabilityConfig
import party.morino.moripautils.velocity.di.VelocityModule
import party.morino.moripautils.velocity.observability.metrics.ConnectionEventListener
import party.morino.moripautils.velocity.observability.metrics.ProxyInfoCollector
import party.morino.moripautils.velocity.observability.metrics.ProxyPlayerMetricsCollector
import java.io.IOException
import java.nio.file.Path

/**
 * MoripaUtils の Velocity 向けプラグイン本体 (現在は observability 機能としてプロキシのメトリクスを Prometheus 形式で公開する)
 *
 * コンストラクタ引数は Velocity (Guice) が注入する。ここ以外ではコンストラクタインジェクションを使わず、
 * 自前のクラスは Koin の `by inject()` / `get()` で依存を取得する。
 */
@Plugin(
    id = "moripa-utils",
    name = "MoripaUtils",
    // バージョンは Gradle が生成する BuildConstants から取得する (gradle.properties の version と連動)
    version = BuildConstants.VERSION,
    description = "morinoparty utility plugin (observability: proxy metrics for Prometheus / Grafana)",
    authors = ["morinoparty"],
)
class MoripaUtils @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    // config.conf を配置するプラグイン専用のデータフォルダ (plugins/moripa-utils)
    @DataDirectory private val dataDirectory: Path,
) : MoripaUtilsKoinComponent {

    @Subscribe
    @Suppress("UnusedParameter")
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        val config = MoripaUtilsConfigLoader(dataDirectory, DEFAULT_CONFIG).load()
        setupKoin(config)
        MoripaUtilsCommon.init()

        val commandManager = VelocityCommandManager<CommandSource>(
            server.pluginManager.ensurePluginContainer(this),
            server,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.identity(),
        )

        // TODO: 各機能のコマンド (設定リロードなど) はここで commandManager に登録する

        // チケット機能は Paper 専用のため、Velocity では observability 機能だけを扱う
        if (config.observability.enabled) {
            startMetricsExporter(config.observability)
        } else {
            logger.info("Observability is disabled in config.conf")
        }

        logger.info("MoripaUtils has been enabled!")
    }

    @Subscribe
    @Suppress("UnusedParameter")
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        // 設定の読み込みに失敗して Koin が起動していない場合もあるので、存在する場合だけ停止する
        MoripaUtilsKoinContext.getOrNull()?.getOrNull<MetricsExporter>()?.stop()
        // 専用コンテナを閉じる (他プラグインの Koin には影響しない)
        MoripaUtilsKoinContext.stop()
        logger.info("MoripaUtils has been disabled!")
    }

    /**
     * コレクターを組み立ててイベントリスナーを登録し、メトリクスの HTTP サーバーを起動する
     *
     * ポートのバインドに失敗してもプロキシ自体の動作には影響しないため、エラーログを出すだけで続行する。
     *
     * @param config observability 機能の設定
     */
    private fun startMetricsExporter(config: ObservabilityConfig) {
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
                "Failed to bind metrics HTTP server on ${config.http.host}:${config.http.port} - is the port in use?",
                e,
            )
            return
        }

        // ポートに 0 を指定した場合に備えて、実際にバインドされたポートを表示する
        val port = get<MetricsHttpServer>().port ?: config.http.port
        logger.info("Metrics are exposed at http://${config.http.host}:$port${config.http.path}")
    }

    /**
     * このプラグイン専用の Koin コンテナを起動する
     *
     * 他プラグインと GlobalContext を共有しないよう、[MoripaUtilsKoinContext] に独立したコンテナを作る。
     *
     * @param config 読み込み済みの設定
     */
    private fun setupKoin(config: MoripaUtilsConfig) {
        val modules = listOf(
            CommonModule.create(config),
            VelocityModule.create(this, server, logger),
        )
        MoripaUtilsKoinContext.start(modules)
    }

    companion object {
        /**
         * Velocity 向けの既定設定
         *
         * Paper (9225) と同じホストで動かしても衝突しないよう既定ポートは 9226 にし、server も proxy にする。
         */
        private val DEFAULT_CONFIG = MoripaUtilsConfig(
            server = "proxy",
            observability = ObservabilityConfig(http = HttpServerConfig(port = 9226)),
        )
    }
}
