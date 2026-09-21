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
    // Kotlin ソースの @Plugin を Velocity のアノテーションプロセッサで処理し velocity-plugin.json を生成する
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.shadow)
}

group = "party.morino"
version = project.version.toString()

dependencies {
    implementation(project(":common"))
    implementation(project(":api"))
    compileOnly(libs.velocity.api)
    // annotationProcessor は Java ソースにしか効かないため、Kotlin では kapt を使う
    kapt(libs.velocity.api)

    implementation(libs.bundles.commands.velocity)

    // compileOnly
    compileOnly(libs.kotlinx.serialization.json)
    compileOnly(libs.bundles.coroutines.velocity)
    compileOnly(kotlin("stdlib-jdk8"))

    // JARにバンドル
    implementation(libs.koin.core)

    // テスト依存関係
    testImplementation(libs.bundles.junit.jupiter)
    testImplementation(libs.bundles.koin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.coroutines.velocity)
    testImplementation(libs.koin.core)
    testImplementation(kotlin("stdlib-jdk8"))
}

tasks {
    build {
        dependsOn("shadowJar")
    }
    shadowJar {
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:.*:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-bom:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-serialization-.*:.*"))
        }
    }
    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
