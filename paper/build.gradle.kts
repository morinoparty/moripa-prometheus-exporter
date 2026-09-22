/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory)
}

group = "party.morino"
version = project.version.toString()

dependencies {
    implementation(project(":common"))
    implementation(project(":api"))
    compileOnly(libs.paper.api)

    implementation(libs.bundles.commands.paper)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.coroutines.bukkit)

    // JARにバンドル
    implementation(libs.koin.core)
    // Prometheus クライアント (レジストリ / HTTP エクスポーター / JVM メトリクス)
    implementation(libs.bundles.prometheus)

    // テスト依存関係
    testImplementation(libs.paper.api)
    testImplementation(libs.bundles.junit.jupiter)
    testImplementation(libs.bundles.koin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.bukkit)
    // スクレイプ結果をテキスト形式で検証するために使う
    testImplementation(libs.prometheus.textformats)
}

tasks {
    build {
        dependsOn("shadowJar")
    }
    shadowJar {
        // 他プラグインが同梱する Prometheus クライアントとクラスが衝突しないようにパッケージを移動する
        relocate("io.prometheus.metrics", "party.morino.prometheusexporter.libs.io.prometheus.metrics")
        // 依存ライブラリのライセンスファイルが JAR 直下で重複しないよう除外する
        exclude("META-INF/LICENSE", "META-INF/NOTICE")
    }
    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
    runServer {
        minecraftVersion("26.2")
    }
}

sourceSets.main {
    resourceFactory {
        paperPluginYaml {
            name = rootProject.name
            version = project.version.toString()
            website = "https://github.com/morinoparty/moripa-prometheus-exporter"
            main = "$group.prometheusexporter.paper.MoripaPrometheusExporter"
            bootstrapper = "$group.prometheusexporter.paper.MoripaPrometheusExporterBootstrap"
            loader = "$group.prometheusexporter.paper.MoripaPrometheusExporterLoader"
            apiVersion = "26.2"
        }
    }
}
