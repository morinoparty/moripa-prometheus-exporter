/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.paper.ticket.mineauth

import org.koin.core.component.inject
import party.morino.mineauth.api.CallerType
import party.morino.mineauth.api.annotations.Authenticated
import party.morino.mineauth.api.annotations.Caller
import party.morino.mineauth.api.annotations.Get
import party.morino.mineauth.api.annotations.Path
import party.morino.mineauth.api.annotations.Query
import party.morino.mineauth.api.auth.Principal
import party.morino.mineauth.api.http.HttpError
import party.morino.mineauth.api.http.HttpStatus
import party.morino.moripautils.common.di.MoripaUtilsKoinComponent
import party.morino.moripautils.common.model.config.TicketConfig
import party.morino.moripautils.common.model.ticket.TicketSearchQuery
import party.morino.moripautils.common.model.ticket.TicketStatus
import party.morino.moripautils.common.ticket.TicketRepository
import party.morino.moripautils.paper.model.ticket.TicketListResponse
import party.morino.moripautils.paper.model.ticket.TicketResponse
import party.morino.moripautils.paper.ticket.TicketPermissions
import java.util.UUID

/**
 * MineAuth に登録するチケットの HTTP API ハンドラー
 *
 * /api/v1/plugins/moripautils 配下にエンドポイントを提供する。
 * MineAuth の API クラスを参照するため、MineAuth が存在することを確認した後にだけ生成すること。
 */
class TicketApiHandler : MoripaUtilsKoinComponent {
    private val repository: TicketRepository by inject()
    private val config: TicketConfig by inject()

    /**
     * チケット一覧をカーソル方式で取得する (スタッフ向け)
     * GET /tickets?category={id}&status={status}&cursor={id}&limit={limit}
     *
     * ユーザートークンは moripautils.ticket.staff 権限が必要。サービストークンは権限チェックの対象外。
     *
     * @param category 絞り込むカテゴリー id (省略時はすべて)
     * @param status 絞り込む状態 (OPEN / CLOSED、大文字小文字は区別しない。省略時はすべて)
     * @param cursor この id より後のチケットを返す (省略時は先頭から)
     * @param limit 取得件数 (省略時は [DEFAULT_LIMIT]、[MAX_LIMIT] を超える値は切り詰める)
     * @return チケット一覧
     */
    @Get("/tickets")
    @Authenticated(permission = TicketPermissions.STAFF, callers = [CallerType.USER, CallerType.SERVICE])
    suspend fun listTickets(
        @Query("category") category: String?,
        @Query("status") status: String?,
        @Query("cursor") cursor: Long?,
        @Query("limit") limit: Int?,
    ): TicketListResponse = search(
        categoryId = category?.takeIf { it.isNotBlank() },
        status = parseStatus(status),
        playerUuid = null,
        cursor = cursor,
        limit = resolveLimit(limit),
    )

    /**
     * チケットを 1 件取得する (スタッフ向け)
     * GET /tickets/{id}
     *
     * @param id チケットの id
     * @return チケット
     * @throws HttpError チケットが存在しない場合 (404)
     */
    @Get("/tickets/{id}")
    @Authenticated(permission = TicketPermissions.STAFF, callers = [CallerType.USER, CallerType.SERVICE])
    suspend fun getTicket(
        @Path("id") id: Long,
    ): TicketResponse {
        val ticket = repository.findById(id)
            ?: throw HttpError(HttpStatus.NOT_FOUND, "Ticket not found: $id", code = "ticket_not_found")
        return TicketResponse.from(ticket, categoryName(ticket.categoryId))
    }

    /**
     * 認証したプレイヤー自身のチケット一覧を取得する
     * GET /me/tickets?cursor={id}&limit={limit}
     *
     * 本人のチケットしか返さないため、権限は不要 (ユーザートークンのみ)。
     *
     * @param principal 認証したプレイヤー
     * @param cursor この id より後のチケットを返す (省略時は先頭から)
     * @param limit 取得件数 (省略時は [DEFAULT_LIMIT]、[MAX_LIMIT] を超える値は切り詰める)
     * @return チケット一覧
     */
    @Get("/me/tickets")
    @Authenticated(callers = [CallerType.USER])
    suspend fun listMyTickets(
        @Caller principal: Principal.User,
        @Query("cursor") cursor: Long?,
        @Query("limit") limit: Int?,
    ): TicketListResponse = search(
        categoryId = null,
        status = null,
        playerUuid = principal.uuid,
        cursor = cursor,
        limit = resolveLimit(limit),
    )

    /**
     * 条件に一致するチケットを 1 ページ分取得する
     *
     * 1 件多く取得して、次のページがあるかどうかを判定する。
     *
     * @param categoryId 絞り込むカテゴリー id
     * @param status 絞り込む状態
     * @param playerUuid 絞り込む送信者
     * @param cursor この id より後のチケットを返す
     * @param limit 1 ページの件数
     * @return チケット一覧
     */
    private suspend fun search(
        categoryId: String?,
        status: TicketStatus?,
        playerUuid: UUID?,
        cursor: Long?,
        limit: Int,
    ): TicketListResponse {
        val fetched = repository.search(
            TicketSearchQuery(
                categoryId = categoryId,
                status = status,
                playerUuid = playerUuid,
                afterId = cursor,
                limit = limit + 1,
            ),
        )
        val hasMore = fetched.size > limit
        val page = fetched.take(limit)
        return TicketListResponse(
            tickets = page.map { TicketResponse.from(it, categoryName(it.categoryId)) },
            // 次のページは今回の最後の id より後から始まる
            nextCursor = if (hasMore) page.last().id else null,
            hasMore = hasMore,
        )
    }

    /**
     * カテゴリー id から表示名を引く
     *
     * @param categoryId カテゴリー id
     * @return 表示名 (設定から削除されたカテゴリーなら null)
     */
    private fun categoryName(categoryId: String): String? = config.categories.firstOrNull { it.id == categoryId }?.name

    companion object {
        /** 1 ページの既定の件数 */
        const val DEFAULT_LIMIT: Int = 50

        /** 1 ページの最大件数 (これを超える指定は切り詰める) */
        const val MAX_LIMIT: Int = 200

        /**
         * status クエリを状態に変換する
         *
         * @param status クエリの値 (null や空文字なら絞り込まない)
         * @return 状態、絞り込まない場合は null
         * @throws HttpError 未知の状態名の場合 (400)
         */
        fun parseStatus(status: String?): TicketStatus? {
            if (status.isNullOrBlank()) return null
            return TicketStatus.entries.firstOrNull { it.name.equals(status.trim(), ignoreCase = true) }
                ?: throw HttpError(
                    HttpStatus.BAD_REQUEST,
                    "Unknown ticket status: $status (allowed: ${TicketStatus.entries.joinToString(", ")})",
                    code = "invalid_status",
                )
        }

        /**
         * limit クエリを実際に使う件数に変換する
         *
         * @param limit クエリの値 (null なら既定値)
         * @return 1 以上 [MAX_LIMIT] 以下の件数
         * @throws HttpError 0 以下が指定された場合 (400)
         */
        fun resolveLimit(limit: Int?): Int {
            if (limit == null) return DEFAULT_LIMIT
            if (limit <= 0) {
                throw HttpError(HttpStatus.BAD_REQUEST, "limit must be greater than 0", code = "invalid_limit")
            }
            return limit.coerceAtMost(MAX_LIMIT)
        }
    }
}
