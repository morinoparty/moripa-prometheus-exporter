/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.velocity.di

import com.velocitypowered.api.proxy.ProxyServer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.slf4j.Logger
import party.morino.prometheusexporter.velocity.MoripaPrometheusExporter

/**
 * Velocity 固有の Koin モジュールを生成するファクトリ
 */
object VelocityModule {
    /**
     * Velocity モジュールを生成する
     *
     * @param plugin Guice が生成したプラグイン本体
     * @param server Guice から注入されたプロキシサーバー
     * @param logger Guice から注入されたプラグイン用ロガー
     * @return プラグイン / プロキシ / ロガーをシングルトンとして提供する Koin モジュール
     */
    fun create(plugin: MoripaPrometheusExporter, server: ProxyServer, logger: Logger): Module = module {
        // Guice が生成した各インスタンスをそのまま Koin から取り出せるようにする
        single<MoripaPrometheusExporter> { plugin }
        single<ProxyServer> { server }
        single<Logger> { logger }
    }
}
