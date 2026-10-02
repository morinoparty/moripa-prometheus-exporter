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
 * チケットを保存するデータベースの設定
 *
 * @property file SQLite のデータベースファイル名 (プラグインのデータフォルダからの相対パス)
 * @throws IllegalArgumentException file が空の場合
 */
@Serializable
data class TicketDatabaseConfig(
    val file: String = "tickets.db",
) {
    init {
        // 空文字だとデータフォルダ自体を開こうとしてしまうため弾く
        require(file.isNotBlank()) { "ticket.database.file must not be blank" }
    }
}
