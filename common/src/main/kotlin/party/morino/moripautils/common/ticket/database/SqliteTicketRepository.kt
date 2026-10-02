/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.ticket.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import party.morino.moripautils.common.model.ticket.Ticket
import party.morino.moripautils.common.model.ticket.TicketSearchQuery
import party.morino.moripautils.common.model.ticket.TicketStatus
import party.morino.moripautils.common.model.ticket.TicketSubmission
import party.morino.moripautils.common.ticket.TicketRepository
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * SQLite (Exposed) にチケットを保存するリポジトリ
 *
 * 接続とテーブル作成は最初の利用時に I/O スレッド上で行い、メインスレッドでのファイル I/O を避ける。
 * SQLite は同時書き込みに弱いため、すべての操作を並列度 1 のディスパッチャーで直列に実行する。
 *
 * @param databaseFile SQLite のデータベースファイル (親ディレクトリが無ければ作成する)
 */
class SqliteTicketRepository(
    private val databaseFile: Path,
) : TicketRepository {
    /** DB 操作専用のディスパッチャー (I/O スレッドを使いつつ、SQLite へのアクセスを 1 本に絞る) */
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    /** 接続済みのデータベース (初回アクセス時に接続とテーブル作成を行う) */
    private val databaseDelegate = lazy { connect() }
    private val database: Database by databaseDelegate

    override suspend fun create(submission: TicketSubmission): Ticket = dbQuery {
        // SQLite の timestamp はミリ秒精度で保存されるため、戻り値と DB の値がずれないよう先に丸める
        val createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val id = TicketsTable.insert {
            it[serverId] = submission.serverId
            it[playerUuid] = submission.playerUuid.toString()
            it[playerName] = submission.playerName
            it[categoryId] = submission.categoryId
            it[content] = submission.content
            it[status] = TicketStatus.OPEN
            it[TicketsTable.createdAt] = createdAt
        } get TicketsTable.id
        Ticket(
            id = id,
            serverId = submission.serverId,
            playerUuid = submission.playerUuid,
            playerName = submission.playerName,
            categoryId = submission.categoryId,
            content = submission.content,
            status = TicketStatus.OPEN,
            createdAt = createdAt,
        )
    }

    override suspend fun findById(id: Long): Ticket? = dbQuery {
        TicketsTable
            .selectAll()
            .where { TicketsTable.id eq id }
            .singleOrNull()
            ?.toTicket()
    }

    override suspend fun search(query: TicketSearchQuery): List<Ticket> = dbQuery {
        val statement = TicketsTable.selectAll()
        // 指定された条件だけを AND で積み重ねる
        query.categoryId?.let { categoryId -> statement.andWhere { TicketsTable.categoryId eq categoryId } }
        query.status?.let { status -> statement.andWhere { TicketsTable.status eq status } }
        query.playerUuid?.let { uuid -> statement.andWhere { TicketsTable.playerUuid eq uuid.toString() } }
        query.afterId?.let { afterId -> statement.andWhere { TicketsTable.id greater afterId } }
        statement
            .orderBy(TicketsTable.id to SortOrder.ASC)
            .limit(query.limit)
            .map { it.toTicket() }
    }

    override fun close() {
        // 一度も使われていなければ接続も存在しないので何もしない
        if (databaseDelegate.isInitialized()) {
            TransactionManager.closeAndUnregister(database)
        }
    }

    /**
     * データベースに接続し、テーブルが無ければ作成する
     *
     * @return 接続済みのデータベース
     */
    private fun connect(): Database {
        // tickets.db をサブディレクトリに置く設定にも対応する
        databaseFile.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        val db = Database.connect(
            url = "jdbc:sqlite:${databaseFile.toAbsolutePath()}",
            driver = "org.sqlite.JDBC",
            // SQLite は Exposed 既定の分離レベルに対応していないため SERIALIZABLE を指定する
            databaseConfig = DatabaseConfig { defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE },
        )
        transaction(db) {
            SchemaUtils.create(TicketsTable)
        }
        return db
    }

    /**
     * DB 専用のディスパッチャー上でトランザクションを実行する
     *
     * 他のプラグインの Exposed 設定に影響されないよう、必ずこのリポジトリの [database] を明示して使う。
     *
     * @param T 処理の戻り値の型
     * @param block トランザクション内で実行する処理
     * @return 処理の結果
     */
    private suspend fun <T> dbQuery(block: () -> T): T = withContext(dispatcher) {
        transaction(database) { block() }
    }

    /**
     * 取得した行をドメインモデルへ変換する
     *
     * @return 変換したチケット
     */
    private fun ResultRow.toTicket(): Ticket = Ticket(
        id = this[TicketsTable.id],
        serverId = this[TicketsTable.serverId],
        playerUuid = UUID.fromString(this[TicketsTable.playerUuid]),
        playerName = this[TicketsTable.playerName],
        categoryId = this[TicketsTable.categoryId],
        content = this[TicketsTable.content],
        status = this[TicketsTable.status],
        createdAt = this[TicketsTable.createdAt],
    )
}
