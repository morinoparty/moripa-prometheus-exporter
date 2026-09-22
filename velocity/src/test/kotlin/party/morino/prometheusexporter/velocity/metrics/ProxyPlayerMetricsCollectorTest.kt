/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.velocity.metrics

import com.velocitypowered.api.plugin.PluginManager
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.config.ProxyConfig
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.mockk.every
import io.mockk.mockk
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import java.net.InetSocketAddress

class ProxyPlayerMetricsCollectorTest {

    @BeforeEach
    fun setUp() {
        // プレイヤーが 2 人接続している "lobby" サーバーを用意する
        val lobby = mockk<RegisteredServer>()
        every { lobby.serverInfo } returns ServerInfo("lobby", InetSocketAddress.createUnresolved("localhost", 25566))
        every { lobby.playersConnected } returns listOf(mockk<Player>(), mockk<Player>())

        val configuration = mockk<ProxyConfig>()
        every { configuration.showMaxPlayers } returns 100

        val pluginManager = mockk<PluginManager>()
        every { pluginManager.plugins } returns emptyList()

        // プロキシ全体では 3 人が接続している状態にする
        val server = mockk<ProxyServer>()
        every { server.playerCount } returns 3
        every { server.configuration } returns configuration
        every { server.allServers } returns listOf(lobby)
        every { server.pluginManager } returns pluginManager

        startKoin {
            modules(module { single<ProxyServer> { server } })
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    @DisplayName("Exposes proxy and backend player counts")
    fun exposesPlayerCounts() {
        val registry = PrometheusRegistry()
        ProxyPlayerMetricsCollector().register(registry)

        // スクレイプ結果をテキスト形式にして値を検証する
        val text = PrometheusTextFormatWriter.create().toDebugString(registry.scrape())

        assertTrue(text.contains("velocity_players_online 3.0"), text)
        assertTrue(text.contains("velocity_backend_players{server=\"lobby\"} 2.0"), text)
        assertTrue(text.contains("velocity_show_max_players 100.0"), text)
    }
}
