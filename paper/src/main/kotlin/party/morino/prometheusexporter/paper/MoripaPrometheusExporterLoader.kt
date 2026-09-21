/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository
import party.morino.prometheusexporter.common.BuildConstants

@Suppress("unused")
class MoripaPrometheusExporterLoader : PluginLoader {
    override fun classloader(classpathBuilder: PluginClasspathBuilder) {
        val resolver = MavenLibraryResolver()
        // ビルドに使った Kotlin と同じバージョンの stdlib を解決する (gradle/libs.versions.toml の kotlin と連動)
        resolver.addDependency(
            Dependency(DefaultArtifact("org.jetbrains.kotlin:kotlin-stdlib:${BuildConstants.KOTLIN_VERSION}"), null),
        )
        resolver.addRepository(
            RemoteRepository.Builder("paper", "default", "https://repo.papermc.io/repository/maven-public/").build(),
        )
        classpathBuilder.addLibrary(resolver)
    }
}
