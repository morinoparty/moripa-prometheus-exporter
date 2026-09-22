/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper

import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockbukkit.mockbukkit.ServerMock
import party.morino.prometheusexporter.paper.metrics.MetricsSampler
import kotlin.test.assertTrue

/**
 * プラグイン有効化後にサンプリングしたメトリクスがレジストリから読み出せることを検証する
 *
 * MockBukkit が未実装のメソッド (Server#getTPS / getAverageTickTime / getServerTickManager と
 * World#getTileEntityCount / getTickableTileEntityCount) に依存するメトリクス (tick / world コレクター) は
 * サンプラーが警告ログを出して読み飛ばすため、ここでは MockBukkit が値を返せるメトリクスだけを検証する。
 */
@ExtendWith(MoripaPrometheusExporterTest::class)
class PaperMetricsTest : KoinTest {

    private val server: ServerMock by inject()
    private val registry: PrometheusRegistry by inject()
    private val sampler: MetricsSampler by inject()

    /** レジストリの内容を Prometheus のテキスト形式に変換する */
    private fun scrapeText(): String = PrometheusTextFormatWriter.create().toDebugString(registry.scrape())

    @Test
    @DisplayName("Sampled server metrics are exposed after sampling")
    fun sampledServerMetricsAreExposed() {
        // プレイヤーを 1 人参加させ、最大人数を変更してからサンプリングする (他のテストの参加分も含めて現在のオンライン数と比較する)
        server.addPlayer()
        server.maxPlayers = 30
        sampler.sampleAll()

        val text = scrapeText()
        val expectedOnline = server.onlinePlayers.size.toDouble()
        assertTrue(text.contains("minecraft_players_online $expectedOnline"), text)
        assertTrue(text.contains("minecraft_players_max 30.0"), text)
        assertTrue(text.contains("minecraft_server_info{"), text)
        // MockBukkit にはこのプラグインだけがロードされ、有効化されている
        assertTrue(text.contains("minecraft_plugins_loaded 1.0"), text)
        assertTrue(text.contains("minecraft_plugins_enabled 1.0"), text)
    }

    @Test
    @DisplayName("Player join event increments the join counter")
    fun playerJoinIncrementsCounter() {
        server.addPlayer()

        val text = scrapeText()
        // PlayerEventListener がイベントを購読していればカウンターが増えている
        assertTrue(Regex("minecraft_player_joins_total [1-9]").containsMatchIn(text), text)
    }
}
