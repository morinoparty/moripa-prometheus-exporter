/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.paper

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.CancellationException
import org.bukkit.event.Listener
import org.incendo.cloud.paper.PaperCommandManager
import org.koin.core.component.get
import party.morino.moripautils.common.MoripaUtilsCommon
import party.morino.moripautils.common.config.MoripaUtilsConfigLoader
import party.morino.moripautils.common.di.CommonModule
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import party.morino.moripautils.common.di.MoripaUtilsKoinContext
import party.morino.moripautils.common.model.config.MoripaUtilsConfig
import party.morino.moripautils.common.model.config.ObservabilityConfig
import party.morino.moripautils.common.model.config.TicketConfig
import party.morino.moripautils.common.observability.http.MetricsHttpServer
import party.morino.moripautils.common.observability.metrics.JvmMetricsCollector
import party.morino.moripautils.common.observability.metrics.MetricsCollector
import party.morino.moripautils.common.observability.metrics.MetricsExporter
import party.morino.moripautils.common.observability.metrics.SampledMetricsCollector
import party.morino.moripautils.common.ticket.TicketRepository
import party.morino.moripautils.common.ticket.TicketService
import party.morino.moripautils.common.ticket.di.TicketModule
import party.morino.moripautils.paper.di.PaperModule
import party.morino.moripautils.paper.observability.metrics.ChunkEventListener
import party.morino.moripautils.paper.observability.metrics.MetricsSampler
import party.morino.moripautils.paper.observability.metrics.PlayerEventListener
import party.morino.moripautils.paper.observability.metrics.PlayerMetricsCollector
import party.morino.moripautils.paper.observability.metrics.ServerInfoCollector
import party.morino.moripautils.paper.observability.metrics.TickDurationListener
import party.morino.moripautils.paper.observability.metrics.TickMetricsCollector
import party.morino.moripautils.paper.observability.metrics.WorldMetricsCollector
import party.morino.moripautils.paper.ticket.di.PaperTicketModule
import party.morino.moripautils.paper.ticket.mineauth.TicketMineAuthIntegration
import java.io.IOException

/**
 * MoripaUtils の Paper 向けプラグイン本体
 *
 * 有効化時に設定を読み込み、このプラグイン専用の Koin コンテナを起動して各機能を初期化する。
 * observability 機能が有効な場合は、メトリクスコレクターを登録して Prometheus 用の HTTP サーバーと
 * メインスレッドのサンプラーを起動する。
 * ticket 機能が有効な場合は、/ticket が使うサービスを読み込み、MineAuth があれば HTTP API を登録する。
 *
 * @property commandManager ブートストラップ段階で生成した Cloud のコマンドマネージャー。
 *   MockBukkit のテストなどブートストラッパーを経由せずに生成された場合は null
 */
open class MoripaUtils(
    val commandManager: PaperCommandManager<CommandSourceStack>? = null,
) : SuspendingJavaPlugin(),
    MoripaUtilsKoinComponent {

    override suspend fun onEnableAsync() {
        val config = loadConfig()
        setupKoin(config)
        MoripaUtilsCommon.init()

        // 機能ごとに設定で有効化されている場合だけ起動する
        if (config.observability.enabled) {
            startObservability(config.observability)
        } else {
            logger.info("Observability is disabled in config.conf")
        }
        if (config.ticket.enabled) {
            startTicket(config.ticket)
        } else {
            logger.info("Ticket is disabled in config.conf")
        }

        logger.info("${pluginMeta.name} v${pluginMeta.version} has been enabled!")
    }

    override suspend fun onDisableAsync() {
        // 有効化に失敗して Koin やモジュールが存在しない場合もあるため、定義があるときだけ停止処理を行う
        MoripaUtilsKoinContext.getOrNull()?.let { koin ->
            koin.getOrNull<MetricsSampler>()?.stop()
            koin.getOrNull<MetricsExporter>()?.stop()
            // ticket 機能が無効な場合はリポジトリが定義されていない
            koin.getOrNull<TicketService>()?.close()
            koin.getOrNull<TicketRepository>()?.close()
        }
        // 専用コンテナを閉じる (他プラグインの Koin には影響しない)
        MoripaUtilsKoinContext.stop()
        logger.info("${pluginMeta.name} has been disabled!")
    }

    /**
     * データフォルダの config.conf を読み込む (存在しなければ既定値を書き出す)
     *
     * @return 読み込んだ設定
     * @throws IllegalStateException config.conf の内容が不正な場合 (プラグインの有効化を失敗させる)
     */
    private fun loadConfig(): MoripaUtilsConfig {
        val loader = MoripaUtilsConfigLoader(dataFolder.toPath(), MoripaUtilsConfig())
        return try {
            loader.load()
        } catch (e: IllegalStateException) {
            // 壊れた設定で黙って既定値を使うより、起動を止めて気付いてもらう
            logger.severe("Failed to load ${MoripaUtilsConfigLoader.CONFIG_FILE_NAME}: ${e.message}")
            throw e
        }
    }

    /**
     * このプラグイン専用の Koin コンテナを起動する
     *
     * 他プラグインと GlobalContext を共有しないよう、[MoripaUtilsKoinContext] に独立したコンテナを作る。
     *
     * @param config 読み込み済みの設定
     */
    private fun setupKoin(config: MoripaUtilsConfig) {
        MoripaUtilsKoinContext.start(
            listOf(
                CommonModule.create(config),
                PaperModule.create(this),
            ),
        )
    }

    /**
     * ticket 機能 (/ticket によるお問い合わせ) を起動する
     *
     * /ticket コマンド自体はブートストラップ段階で登録済み ([MoripaUtilsBootstrap])。ここではコマンドが使う
     * サービスなどを Koin に読み込み、MineAuth があれば HTTP API を登録する。
     * データベースへの接続は最初のチケット操作時に I/O スレッドで行う。
     *
     * @param config ticket 機能の設定
     */
    private fun startTicket(config: TicketConfig) {
        MoripaUtilsKoinContext.loadModules(
            listOf(
                TicketModule.create(dataFolder.toPath(), config, logger),
                PaperTicketModule.create(),
            ),
        )
        // MineAuth は任意依存。API クラスに触れる前に Bukkit の API だけで存在を確認する
        if (server.pluginManager.getPlugin(MINEAUTH_PLUGIN_NAME) != null) {
            registerMineAuthSafely()
        } else {
            logger.info("MineAuth is not installed; ticket HTTP endpoints are disabled")
        }
    }

    /**
     * MineAuth 連携を登録する (失敗してもプラグイン全体は止めない)
     *
     * 導入済みの MineAuth が compileOnly の mineauth-api と互換でない場合、クラス解決時に
     * LinkageError (NoClassDefFoundError / NoSuchMethodError など) が発生する。
     * これが onEnableAsync の外へ漏れると、起動済みの observability ごとプラグインが無効化されるため、ここで握りつぶす。
     */
    @Suppress("TooGenericExceptionCaught")
    private fun registerMineAuthSafely() {
        try {
            TicketMineAuthIntegration().register()
        } catch (e: CancellationException) {
            // コルーチンのキャンセルは握りつぶさず伝播させる
            throw e
        } catch (e: LinkageError) {
            // API の互換性が無い MineAuth が導入されている
            logger.warning("MineAuth integration failed (incompatible MineAuth API?); ticket HTTP endpoints are disabled: $e")
        } catch (e: Exception) {
            // その他の予期しない失敗も ticket の HTTP API だけを諦める
            logger.warning("MineAuth integration failed; ticket HTTP endpoints are disabled: $e")
        }
    }

    /**
     * observability 機能 (Prometheus メトリクスの公開) を起動する
     *
     * @param config observability 機能の設定
     */
    private fun startObservability(config: ObservabilityConfig) {
        val collectors = createCollectors(config)
        // Bukkit のイベントを購読するコレクターはリスナーとして登録する
        collectors.filterIsInstance<Listener>().forEach { listener ->
            server.pluginManager.registerEvents(listener, this)
        }

        startExporter(config, collectors)
        // メインスレッドでしか読めない値は定期的にサンプリングする
        get<MetricsSampler>().start(collectors.filterIsInstance<SampledMetricsCollector>())
    }

    /**
     * 登録するコレクターの一覧を組み立てる
     *
     * @param config observability 機能の設定 (JVM メトリクスの有効 / 無効に使う)
     * @return 登録順に並んだコレクターの一覧
     */
    private fun createCollectors(config: ObservabilityConfig): List<MetricsCollector> = buildList {
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
     * @param config observability 機能の設定 (ログ出力用)
     * @param collectors 登録するコレクターの一覧
     */
    private fun startExporter(config: ObservabilityConfig, collectors: List<MetricsCollector>) {
        val httpConfig = config.http
        try {
            get<MetricsExporter>().start(collectors)
        } catch (e: IOException) {
            logger.severe(
                "Failed to bind metrics HTTP server on ${httpConfig.host}:${httpConfig.port} - is the port in use? (${e.message})",
            )
            return
        }
        // ポートに 0 を指定した場合は自動選択された実際のポートを表示する
        val port = get<MetricsHttpServer>().port ?: httpConfig.port
        logger.info("Metrics are available at http://${httpConfig.host}:$port${httpConfig.path}")
    }

    companion object {
        /** 連携する MineAuth のプラグイン名 */
        private const val MINEAUTH_PLUGIN_NAME = "MineAuth"
    }
}
