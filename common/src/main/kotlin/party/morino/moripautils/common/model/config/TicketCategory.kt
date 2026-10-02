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
 * お問い合わせのカテゴリー
 *
 * @property id データベースに保存する識別子 (英小文字 / 数字 / ハイフン / アンダースコアのみ)。後から変更すると既存のチケットと紐付かなくなる
 * @property name プレイヤーに表示する名前
 * @throws IllegalArgumentException id が使用できない文字を含む場合、または name が空の場合
 */
@Serializable
data class TicketCategory(
    val id: String,
    val name: String,
) {
    init {
        // id はコマンド引数や DB の値として扱うため、表記ゆれの起きない小文字の識別子に限定する
        require(ID_PATTERN.matches(id)) { "ticket category id must match ${ID_PATTERN.pattern}, but was '$id'" }
        // 表示名が空だと UI 上で選択肢を区別できない
        require(name.isNotBlank()) { "ticket category name must not be blank (id: $id)" }
    }

    companion object {
        /** id に使用できる文字のパターン */
        val ID_PATTERN: Regex = Regex("^[a-z0-9_-]+$")
    }
}
