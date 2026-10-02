/*
 * Written in 2026 by Nikomaru <nikomaru@nikomaru.dev>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide.This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package party.morino.moripautils.common.ticket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.dsl.bind
import org.koin.dsl.module
import party.morino.moripautils.common.di.MoripaUtilsKoinContext
import party.morino.moripautils.common.model.config.MoripaUtilsConfig
import party.morino.moripautils.common.model.config.TicketCategory
import party.morino.moripautils.common.model.ticket.Ticket
import party.morino.moripautils.common.model.ticket.TicketSearchQuery
import party.morino.moripautils.common.model.ticket.TicketStatus
import party.morino.moripautils.common.model.ticket.TicketSubmission
import party.morino.moripautils.common.model.ticket.TicketSubmitResult
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

/**
 * [TicketService] の分岐 (カテゴリー不明 / 本文が空 / 成功) を確認するテスト
 *
 * common のテストには mockk が無いため、リポジトリと通知先は記録するだけの手書きの偽物に差し替える。
 */
class TicketServiceTest {
    private val repository = RecordingRepository()
    private val notifier = RecordingNotifier()
    // 通知は非同期に行われるため、完了を待てるように専用の Job を持つスコープを渡す
    private val notificationJob = Job()
    private val service = TicketService(Logger.getLogger("TicketServiceTest"), CoroutineScope(notificationJob))
    private val playerUuid: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val config = MoripaUtilsConfig(server = "test")
        MoripaUtilsKoinContext.start(
            listOf(
                module {
                    single { config }
                    single<TicketRepository> { repository }
                    single { notifier } bind TicketNotifier::class
                },
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        service.close()
        MoripaUtilsKoinContext.stop()
    }

    @Test
    @DisplayName("Rejects an unknown category without saving or notifying")
    fun rejectsUnknownCategory() = runBlocking {
        val result = service.submit(playerUuid, "Steve", "unknown", "hello")

        assertEquals(TicketSubmitResult.UnknownCategory("unknown"), result)
        assertTrue(repository.created.isEmpty())
        assertTrue(notifier.notified.isEmpty())
    }

    @Test
    @DisplayName("Rejects blank content without saving or notifying")
    fun rejectsBlankContent() = runBlocking {
        val result = service.submit(playerUuid, "Steve", "bug", "   \n ")

        assertEquals(TicketSubmitResult.BlankContent, result)
        assertTrue(repository.created.isEmpty())
        assertTrue(notifier.notified.isEmpty())
    }

    @Test
    @DisplayName("Saves the trimmed ticket and notifies with the resolved category")
    fun savesAndNotifies() = runBlocking {
        val result = service.submit(playerUuid, "Steve", "bug", "  block disappeared  ")

        val success = assertInstanceOf(TicketSubmitResult.Success::class.java, result)
        // submit は通知の完了を待たずに返るため、起動された通知の完了を待ってから検証する
        notificationJob.children.toList().joinAll()
        // 送信元サーバーは config.conf の server、本文は前後の空白を取り除いたもの
        assertEquals(
            listOf(TicketSubmission("test", playerUuid, "Steve", "bug", "block disappeared")),
            repository.created,
        )
        assertEquals(listOf(success.ticket to "バグの報告"), notifier.notified.map { it.first to it.second.name })
    }

    /** create の呼び出しを記録し、連番の id を振って返すリポジトリ */
    private class RecordingRepository : TicketRepository {
        val created = mutableListOf<TicketSubmission>()

        override suspend fun create(submission: TicketSubmission): Ticket {
            created += submission
            return Ticket(
                id = created.size.toLong(),
                serverId = submission.serverId,
                playerUuid = submission.playerUuid,
                playerName = submission.playerName,
                categoryId = submission.categoryId,
                content = submission.content,
                status = TicketStatus.OPEN,
                createdAt = Instant.EPOCH,
            )
        }

        override suspend fun findById(id: Long): Ticket? = null

        override suspend fun search(query: TicketSearchQuery): List<Ticket> = emptyList()

        override fun close() = Unit
    }

    /** 通知の呼び出しを記録するだけの通知先 */
    private class RecordingNotifier : TicketNotifier {
        val notified = mutableListOf<Pair<Ticket, TicketCategory>>()

        override suspend fun notify(ticket: Ticket, category: TicketCategory) {
            notified += ticket to category
        }
    }
}
