/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.paper.di

import org.bukkit.Server
import org.koin.core.module.Module
import org.koin.dsl.module
import party.morino.prometheusexporter.paper.MoripaPrometheusExporter
import party.morino.prometheusexporter.paper.metrics.MetricsSampler

/**
 * Paper 固有の Koin モジュールを生成するファクトリ
 *
 * 共通モジュール ([party.morino.prometheusexporter.common.di.CommonModule]) と組み合わせて使う。
 */
object PaperModule {
    /**
     * Paper モジュールを生成する
     *
     * @param plugin 有効化中のプラグインインスタンス
     * @return プラグイン / Bukkit サーバー / サンプラーをシングルトンとして提供する Koin モジュール
     */
    fun create(plugin: MoripaPrometheusExporter): Module = module {
        // プラグイン本体 (ロガーやコルーチンの起動に使う)
        single<MoripaPrometheusExporter> { plugin }
        // コレクターが Bukkit API を読むためのサーバー
        single<Server> { plugin.server }
        // メインスレッド上で定期的にサンプリングを行うサンプラー
        single { MetricsSampler() }
    }
}
