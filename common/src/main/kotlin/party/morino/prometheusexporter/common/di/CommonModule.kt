/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.di

import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.koin.core.module.Module
import org.koin.dsl.module
import party.morino.prometheusexporter.common.http.MetricsHttpServer
import party.morino.prometheusexporter.common.metrics.MetricsExporter
import party.morino.prometheusexporter.common.model.config.ExporterConfig

/**
 * Paper / Velocity 共通の Koin モジュールを生成するファクトリ
 */
object CommonModule {
    /**
     * 共通モジュールを生成する
     *
     * @param config 読み込み済みのエクスポーター設定 (プラットフォーム側で ExporterConfigLoader から取得する)
     * @return 設定 / レジストリ / HTTP サーバー / エクスポーターをシングルトンとして提供する Koin モジュール
     */
    fun create(config: ExporterConfig): Module = module {
        // 読み込み済みの設定をそのまま共有する
        single { config }
        // すべてのコレクターが登録する共通レジストリ
        single { PrometheusRegistry() }
        single { MetricsHttpServer() }
        single { MetricsExporter() }
    }
}
