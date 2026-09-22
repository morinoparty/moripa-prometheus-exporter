/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import party.morino.prometheusexporter.common.model.config.ExporterConfig
import party.morino.prometheusexporter.common.model.config.HttpServerConfig
import java.nio.file.Files
import java.nio.file.Path

class ExporterConfigLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    @DisplayName("Writes defaults when config is missing")
    fun writesDefaultsWhenConfigIsMissing() {
        // データフォルダ自体がまだ存在しない状態から読み込む
        val dataDirectory = tempDir.resolve("plugin")
        val defaults = ExporterConfig(server = HttpServerConfig(port = 9226))
        val loader = ExporterConfigLoader(dataDirectory, defaults)

        val loaded = loader.load()

        // 既定値がそのまま返り、ファイルも書き出されている
        assertEquals(defaults, loaded)
        val configFile = dataDirectory.resolve(ExporterConfigLoader.CONFIG_FILE_NAME)
        assertTrue(Files.exists(configFile))
        // 既定値も含めて書き出されている (encodeDefaults = true)
        val text = Files.readString(configFile)
        assertTrue(text.contains("\"port\": 9226"))
        assertTrue(text.contains("\"samplingIntervalTicks\": 20"))
    }

    @Test
    @DisplayName("Reads existing config and ignores unknown keys")
    fun readsExistingConfigAndIgnoresUnknownKeys() {
        val configFile = tempDir.resolve(ExporterConfigLoader.CONFIG_FILE_NAME)
        // 未知のキーと一部のキーだけを含む設定ファイルを用意する
        Files.writeString(
            configFile,
            """
            {
                "server": { "host": "127.0.0.1", "port": 1234, "unknown": true },
                "metrics": { "jvm": false },
                "extra": "ignored"
            }
            """.trimIndent(),
        )
        val loader = ExporterConfigLoader(tempDir, ExporterConfig())

        val loaded = loader.load()

        // 指定したキーは反映され、省略したキーは既定値になる
        assertEquals("127.0.0.1", loaded.server.host)
        assertEquals(1234, loaded.server.port)
        assertEquals("/metrics", loaded.server.path)
        assertEquals(false, loaded.metrics.jvm)
        assertEquals(20L, loaded.metrics.samplingIntervalTicks)
    }

    @Test
    @DisplayName("Fails when samplingIntervalTicks is not positive")
    fun failsWhenSamplingIntervalTicksIsNotPositive() {
        val configFile = tempDir.resolve(ExporterConfigLoader.CONFIG_FILE_NAME)
        // 0 tick はサンプラーがビジーループになるため、MetricsConfig の init ブロックで拒否される
        Files.writeString(
            configFile,
            """
            { "metrics": { "samplingIntervalTicks": 0 } }
            """.trimIndent(),
        )
        val loader = ExporterConfigLoader(tempDir, ExporterConfig())

        // init ブロックの IllegalArgumentException はローダーが設定ファイルのパス付き IllegalStateException に包み直す
        val exception = assertThrows(IllegalStateException::class.java) { loader.load() }
        assertTrue(exception.message!!.contains(ExporterConfigLoader.CONFIG_FILE_NAME))
    }
}
