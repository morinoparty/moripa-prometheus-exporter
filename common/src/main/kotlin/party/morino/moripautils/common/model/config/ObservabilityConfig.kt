/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.model.config

import kotlinx.serialization.Serializable

/**
 * メトリクス公開 (observability 機能) の設定
 *
 * @property enabled observability 機能を有効にするかどうか。false の場合は HTTP サーバーもコレクターも起動しない
 * @property http メトリクスを公開する HTTP サーバーの設定
 * @property metrics 収集するメトリクスに関する設定
 */
@Serializable
data class ObservabilityConfig(
    val enabled: Boolean = true,
    val http: HttpServerConfig = HttpServerConfig(),
    val metrics: MetricsConfig = MetricsConfig(),
)
