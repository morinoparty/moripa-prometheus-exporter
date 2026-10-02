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
 * 新しいチケットを外部 (Discord など) へ通知する Webhook の設定
 *
 * @property url 通知先の Webhook URL。空文字の場合は通知しない
 */
@Serializable
data class TicketWebhookConfig(
    val url: String = "",
)
