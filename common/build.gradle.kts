/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "party.morino"
version = project.version.toString()

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.velocity.api)
    implementation(libs.kotlinx.serialization.json)
}

// ビルド時の値 (プラグインのバージョン、Kotlin のバージョン) を Kotlin の const val として生成する。
// @Plugin アノテーションの引数や PluginLoader で解決する kotlin-stdlib のバージョンは
// コンパイル時定数である必要があるため、Gradle 側の値を BuildConstants.kt に書き出して参照させる。
// パッケージ名を変更した場合はこの basePackage も合わせて変更すること。
val basePackage = "party.morino.prometheusexporter"
val buildConstantsDir = layout.buildDirectory.dir("generated/sources/buildConstants/kotlin/main")

val generateBuildConstants by tasks.registering {
    val pluginVersion = project.version.toString()
    val kotlinVersion = libs.versions.kotlin.get()
    val packageDir = basePackage.replace('.', '/')
    val outputDir = buildConstantsDir

    // 入力値が変わったときだけ再生成されるようにする
    inputs.property("pluginVersion", pluginVersion)
    inputs.property("kotlinVersion", kotlinVersion)
    inputs.property("basePackage", basePackage)
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().file("$packageDir/common/BuildConstants.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package $basePackage.common
            |
            |/**
            | * Gradle のビルド時に生成される定数
            | * このファイルは common/build.gradle.kts の generateBuildConstants タスクが生成するため、手で編集しないこと
            | */
            |object BuildConstants {
            |    /** プラグインのバージョン (gradle.properties の version) */
            |    const val VERSION = "$pluginVersion"
            |
            |    /** ビルドに使用した Kotlin のバージョン (gradle/libs.versions.toml の kotlin) */
            |    const val KOTLIN_VERSION = "$kotlinVersion"
            |}
            |
            """.trimMargin(),
        )
    }
}

// 生成したソースをコンパイル対象に加える (タスクを渡すことで依存関係も自動的に張られる)
kotlin {
    sourceSets["main"].kotlin.srcDir(generateBuildConstants)
}
