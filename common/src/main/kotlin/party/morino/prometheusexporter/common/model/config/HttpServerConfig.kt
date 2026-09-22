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
 * メトリクスを公開する HTTP サーバーの設定
 *
 * @property host バインドするホスト名 / アドレス (既定はすべてのインターフェース)
 * @property port バインドするポート番号 (0 を指定すると空いているポートが自動で選ばれる)
 * @property path メトリクスを公開するパス (Prometheus の metrics_path に合わせる)
 */
@Serializable
data class HttpServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 9225,
    val path: String = "/metrics",
)
