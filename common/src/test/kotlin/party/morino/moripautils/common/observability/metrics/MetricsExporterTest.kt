/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.observability.metrics

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.core.component.inject
import party.morino.moripautils.common.di.CommonModule
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import party.morino.moripautils.common.di.MoripaUtilsKoinContext
import party.morino.moripautils.common.observability.http.MetricsHttpServer
import party.morino.moripautils.common.model.config.HttpServerConfig
import party.morino.moripautils.common.model.config.MoripaUtilsConfig
import party.morino.moripautils.common.model.config.ObservabilityConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class MetricsExporterTest : MoripaUtilsKoinComponent {

    private val exporter: MetricsExporter by inject()
    private val httpServer: MetricsHttpServer by inject()

    @BeforeEach
    fun setUp() {
        // ポート 0 で空いているポートを自動選択させる
        val config = MoripaUtilsConfig(observability = ObservabilityConfig(http = HttpServerConfig(port = 0)))
        MoripaUtilsKoinContext.start(listOf(CommonModule.create(config)))
    }

    @AfterEach
    fun tearDown() {
        exporter.stop()
        MoripaUtilsKoinContext.stop()
    }

    @Test
    @DisplayName("Serves JVM metrics over HTTP after start")
    fun servesJvmMetricsOverHttp() {
        exporter.start(listOf(JvmMetricsCollector()))

        // 実際にバインドされたポートに対して GET する
        val port = httpServer.port
        assertNotNull(port)
        val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port/metrics")).GET().build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("jvm_memory_used_bytes"))
    }

    @Test
    @DisplayName("Stop releases the port and double stop is a no-op")
    fun stopReleasesPort() {
        exporter.start(listOf(JvmMetricsCollector()))
        assertNotNull(httpServer.port)

        exporter.stop()
        // 停止後はポートが解放され、再度 stop しても例外にならない
        assertEquals(null, httpServer.port)
        exporter.stop()
    }
}
