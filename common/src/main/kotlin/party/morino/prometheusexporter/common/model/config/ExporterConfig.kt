/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.prometheusexporter.common.model.config

import kotlinx.serialization.Serializable

/**
 * エクスポーター全体の設定 (プラグインのデータフォルダにある config.json に対応する)
 *
 * @property server メトリクスを公開する HTTP サーバーの設定
 * @property metrics 収集するメトリクスに関する設定
 */
@Serializable
data class ExporterConfig(
    val server: HttpServerConfig = HttpServerConfig(),
    val metrics: MetricsConfig = MetricsConfig(),
)
