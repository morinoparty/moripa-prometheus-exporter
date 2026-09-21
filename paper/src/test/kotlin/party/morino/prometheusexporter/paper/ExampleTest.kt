/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MoripaPrometheusExporterTest::class)
class ExampleTest : KoinTest {

    private val server: ServerMock by inject()

    @Test
    @DisplayName("Plugin is enabled successfully")
    fun pluginIsEnabled() {
        val plugin = MoripaPrometheusExporterTest.plugin
        assertNotNull(plugin)
        assertTrue(plugin.isEnabled)
    }

    @Test
    @DisplayName("Server has the plugin loaded")
    fun serverHasPlugin() {
        val pluginManager = server.pluginManager
        val plugin = pluginManager.getPlugin("MoripaPrometheusExporter")
        assertNotNull(plugin)
    }
}
