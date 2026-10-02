/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.ticket.webhook

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import party.morino.moripautils.common.model.config.TicketCategory
import party.morino.moripautils.common.model.ticket.Ticket

/**
 * 新しいチケットを知らせる Discord Webhook の本文 (JSON) を組み立てる
 *
 * 外部状態に依存しない純粋関数だけを持つ。
 */
object DiscordWebhookPayload {
    /** Embed の description の最大文字数 (Discord の制限) */
    private const val MAX_DESCRIPTION_LENGTH = 4096

    /** Embed の field の値の最大文字数 (Discord の制限) */
    private const val MAX_FIELD_VALUE_LENGTH = 1024

    /** Embed の色 (緑系) */
    private const val EMBED_COLOR = 0x4CAF50

    /**
     * チケット 1 件分の Webhook 本文を組み立てる
     *
     * @param ticket 通知するチケット
     * @param category チケットのカテゴリー
     * @return Discord の Execute Webhook API に送る JSON
     */
    fun create(ticket: Ticket, category: TicketCategory): JsonObject = buildJsonObject {
        // 本文に @everyone などが含まれていてもメンションが飛ばないようにする
        putJsonObject("allowed_mentions") {
            putJsonArray("parse") {}
        }
        putJsonArray("embeds") {
            addJsonObject {
                put("title", "新しいお問い合わせ #${ticket.id}")
                put("description", ticket.content.take(MAX_DESCRIPTION_LENGTH))
                put("color", EMBED_COLOR)
                put("timestamp", ticket.createdAt.toString())
                putJsonArray("fields") {
                    add(field("カテゴリー", category.name))
                    add(field("プレイヤー", "${ticket.playerName} (${ticket.playerUuid})"))
                    add(field("サーバー", ticket.serverId))
                    add(field("ID", ticket.id.toString()))
                }
            }
        }
    }

    /**
     * Embed の field を 1 つ組み立てる (横並びで表示する)
     *
     * @param name 項目名
     * @param value 値 (長すぎる場合は切り詰める)
     * @return field の JSON
     */
    private fun field(name: String, value: String): JsonObject = buildJsonObject {
        put("name", name)
        put("value", value.take(MAX_FIELD_VALUE_LENGTH))
        put("inline", true)
    }
}
