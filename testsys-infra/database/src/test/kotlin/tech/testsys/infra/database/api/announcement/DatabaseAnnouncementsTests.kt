package tech.testsys.infra.database.api.announcement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.time.Instant

class DatabaseAnnouncementsTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var announcements: Announcements

    @Nested
    inner class CreateTests {

        @Test
        fun `should store the announcement with the text and link trimmed of surrounding whitespace`() {
            val result = announcements.create(
                text = "  Работы в субботу\t",
                link = " https://example.com/news?id=1 \t",
                kind = AnnouncementKind.Warning,
                shownFrom = DAY_1,
                shownUntil = DAY_2,
            )

            val saved = checkNotNull((result as? AnnouncementSave.Saved)?.announcement) { "Not stored: $result" }
            val expected = Announcement(
                id = saved.id,
                text = "Работы в субботу",
                kind = AnnouncementKind.Warning,
                shownFrom = DAY_1,
                shownUntil = DAY_2,
                link = "https://example.com/news?id=1",
            )
            assertEquals(expected, saved)
            assertEquals(listOf(saved), announcements.findAll())
        }

        @ParameterizedTest
        @MethodSource("tech.testsys.infra.database.api.announcement.DatabaseAnnouncementsTests#absentLinks")
        fun `should store no link for a missing or blank link`(link: String?) {
            val result = announcements.create(
                text = "Текст",
                link = link,
                kind = AnnouncementKind.Info,
                shownFrom = DAY_1,
                shownUntil = DAY_2,
            )

            assertEquals(null, (result as? AnnouncementSave.Saved)?.announcement?.link)
            assertEquals(listOf<String?>(null), announcements.findAll().map { announcement -> announcement.link })
        }

        @Test
        fun `should store an upper-case http scheme as entered`() {
            val result = announcements.create(
                text = "Текст",
                link = "HTTP://Example.com/Path",
                kind = AnnouncementKind.Info,
                shownFrom = DAY_1,
                shownUntil = DAY_2,
            )

            assertEquals("HTTP://Example.com/Path", (result as? AnnouncementSave.Saved)?.announcement?.link)
        }

        @Test
        fun `should store a text at the length limit counted in code points`() {
            val text = "😀".repeat(300)

            val result = announcements.create(text = text, link = null, kind = AnnouncementKind.Info, shownFrom = DAY_1, shownUntil = DAY_2)

            assertEquals(text, (result as? AnnouncementSave.Saved)?.announcement?.text)
            assertEquals(listOf(text), announcements.findAll().map { announcement -> announcement.text })
        }

        @Test
        fun `should store a link at the length limit counted in code points`() {
            val link = linkOfCodePoints(2048)

            val result = announcements.create(
                text = "Текст",
                link = link,
                kind = AnnouncementKind.Info,
                shownFrom = DAY_1,
                shownUntil = DAY_2,
            )

            assertEquals(link, (result as? AnnouncementSave.Saved)?.announcement?.link)
            assertEquals(listOf<String?>(link), announcements.findAll().map { announcement -> announcement.link })
        }

        @ParameterizedTest(name = "{4}")
        @MethodSource("tech.testsys.infra.database.api.announcement.DatabaseAnnouncementsTests#refusals")
        fun `should refuse the first failed check without storing anything`(
            text: String,
            link: String?,
            shownFrom: Instant,
            shownUntil: Instant,
            reason: AnnouncementRejection,
        ) {
            val result = announcements.create(
                text = text,
                link = link,
                kind = AnnouncementKind.Info,
                shownFrom = shownFrom,
                shownUntil = shownUntil,
            )

            assertEquals(AnnouncementSave.Rejected(reason), result)
            assertEquals(emptyList<Announcement>(), announcements.findAll())
        }
    }

    @Nested
    inner class UpdateTests {

        @Test
        fun `should replace the text, link, kind and period of the announcement`() {
            val stored = create(text = "Старый текст", shownFrom = DAY_1, shownUntil = DAY_2, link = OLD_LINK)
            val expected = Announcement(
                id = stored.id,
                text = "Новый текст",
                kind = AnnouncementKind.Warning,
                shownFrom = DAY_2,
                shownUntil = DAY_3,
                link = "http://example.org/new",
            )

            val result = announcements.update(
                stored.id,
                text = " Новый текст ",
                link = " http://example.org/new ",
                kind = AnnouncementKind.Warning,
                shownFrom = DAY_2,
                shownUntil = DAY_3,
            )

            assertEquals(AnnouncementSave.Saved(expected), result)
            assertEquals(listOf(expected), announcements.findAll())
        }

        @ParameterizedTest
        @MethodSource("tech.testsys.infra.database.api.announcement.DatabaseAnnouncementsTests#absentLinks")
        fun `should remove the link for a missing or blank link`(link: String?) {
            val stored = create(text = "Текст", shownFrom = DAY_1, shownUntil = DAY_2, link = "https://example.com")

            val result = announcements.update(
                stored.id,
                text = "Текст",
                link = link,
                kind = AnnouncementKind.Info,
                shownFrom = DAY_1,
                shownUntil = DAY_2,
            )

            assertEquals(AnnouncementSave.Saved(stored.copy(link = null)), result)
            assertEquals(listOf(stored.copy(link = null)), announcements.findAll())
        }

        @ParameterizedTest(name = "{4}")
        @MethodSource("tech.testsys.infra.database.api.announcement.DatabaseAnnouncementsTests#refusals")
        fun `should refuse the first failed check and keep the announcement`(
            text: String,
            link: String?,
            shownFrom: Instant,
            shownUntil: Instant,
            reason: AnnouncementRejection,
        ) {
            val stored = create(text = "Текст", shownFrom = DAY_1, shownUntil = DAY_2, link = "https://example.com")

            val result = announcements.update(
                stored.id,
                text = text,
                link = link,
                kind = AnnouncementKind.Warning,
                shownFrom = shownFrom,
                shownUntil = shownUntil,
            )

            assertEquals(AnnouncementSave.Rejected(reason), result)
            assertEquals(listOf(stored), announcements.findAll())
        }

        @Test
        fun `should keep the closing moment of a closed announcement`() {
            val stored = create(text = "Старый текст", shownFrom = DAY_1, shownUntil = DAY_3)
            announcements.close(stored.id, DAY_2)
            val expected = Announcement(
                id = stored.id,
                text = "Новый текст",
                kind = AnnouncementKind.Warning,
                shownFrom = DAY_1,
                shownUntil = DAY_4,
                closedAt = DAY_2,
            )

            val result = announcements.update(
                stored.id,
                text = "Новый текст",
                link = null,
                kind = AnnouncementKind.Warning,
                shownFrom = DAY_1,
                shownUntil = DAY_4,
            )

            assertEquals(AnnouncementSave.Saved(expected), result)
            assertEquals(listOf(expected), announcements.findAll())
        }

        @Test
        fun `should report a missing announcement`() {
            val result = announcements.update(
                AnnouncementId(1),
                text = "Текст",
                link = null,
                kind = AnnouncementKind.Info,
                shownFrom = DAY_1,
                shownUntil = DAY_2,
            )

            assertEquals(AnnouncementSave.NotFound, result)
            assertEquals(emptyList<Announcement>(), announcements.findAll())
        }
    }

    @Nested
    inner class CloseTests {

        @Test
        fun `should close a shown announcement at the moment, keep it in the list and stop showing it`() {
            val closed = create(text = "Закрываемое", shownFrom = DAY_1, shownUntil = DAY_3)
            val kept = create(text = "Оставшееся", shownFrom = DAY_1, shownUntil = DAY_3)

            val isClosed = announcements.close(closed.id, DAY_2)

            assertTrue(isClosed)
            assertEquals(listOf(kept, closed.copy(closedAt = DAY_2)), announcements.findAll())
            assertEquals(listOf(kept), announcements.findActive(DAY_2))
        }

        @Test
        fun `should close an announcement whose showing has not started yet`() {
            val future = create(text = "Будущее", shownFrom = DAY_3, shownUntil = DAY_4)

            val isClosed = announcements.close(future.id, DAY_2)

            assertTrue(isClosed)
            assertEquals(listOf(future.copy(closedAt = DAY_2)), announcements.findAll())
            assertEquals(emptyList<Announcement>(), announcements.findActive(DAY_3))
        }

        @Test
        fun `should refuse an already closed announcement and keep the first closing moment`() {
            val stored = create(text = "Текст", shownFrom = DAY_1, shownUntil = DAY_4)
            announcements.close(stored.id, DAY_2)

            val isClosed = announcements.close(stored.id, DAY_3)

            assertFalse(isClosed)
            assertEquals(listOf(stored.copy(closedAt = DAY_2)), announcements.findAll())
        }

        @ParameterizedTest
        @MethodSource("tech.testsys.infra.database.api.announcement.DatabaseAnnouncementsTests#finishedMoments")
        fun `should refuse an announcement whose showing has ended without changing it`(at: Instant) {
            val stored = create(text = "Текст", shownFrom = DAY_1, shownUntil = DAY_2)

            val isClosed = announcements.close(stored.id, at)

            assertFalse(isClosed)
            assertEquals(listOf(stored), announcements.findAll())
        }

        @Test
        fun `should report a missing announcement`() {
            assertFalse(announcements.close(AnnouncementId(1), DAY_1))
        }
    }

    @Nested
    inner class FindAllTests {

        @Test
        fun `should return future and finished announcements with the latest start first and the larger id first on a tie`() {
            val finished = create(text = "Завершённое", shownFrom = DAY_1, shownUntil = DAY_2)
            val firstFuture = create(text = "Будущее 1", shownFrom = DAY_3, shownUntil = DAY_4)
            val secondFuture = create(text = "Будущее 2", shownFrom = DAY_3, shownUntil = DAY_4)

            val result = announcements.findAll()

            assertEquals(listOf(secondFuture, firstFuture, finished), result)
        }
    }

    @Nested
    inner class FindActiveTests {

        @Test
        fun `should return the announcements shown from the start inclusive to the end exclusive`() {
            val startingNow = create(text = "Начинается сейчас", shownFrom = DAY_2, shownUntil = DAY_3)
            val running = create(text = "Идёт", shownFrom = DAY_1, shownUntil = DAY_3)
            create(text = "Закончилось сейчас", shownFrom = DAY_1, shownUntil = DAY_2)
            create(text = "Будущее", shownFrom = DAY_2.plusSeconds(1), shownUntil = DAY_3)

            val result = announcements.findActive(DAY_2)

            assertEquals(listOf(startingNow, running), result)
        }

        @Test
        fun `should return at most three announcements with the latest start first and the larger id first on a tie`() {
            create(text = "Самое раннее", shownFrom = DAY_1, shownUntil = DAY_4)
            val earlier = create(text = "Раннее", shownFrom = DAY_1.plusSeconds(1), shownUntil = DAY_4)
            val first = create(text = "Первое", shownFrom = DAY_2, shownUntil = DAY_4)
            val second = create(text = "Второе", shownFrom = DAY_2, shownUntil = DAY_4)

            val result = announcements.findActive(DAY_3)

            assertEquals(listOf(second, first, earlier), result)
        }

        @Test
        fun `should not return a closed announcement within its period`() {
            val closed = create(text = "Закрытое", shownFrom = DAY_1, shownUntil = DAY_4)
            val running = create(text = "Идёт", shownFrom = DAY_1, shownUntil = DAY_4)
            announcements.close(closed.id, DAY_2)

            val result = announcements.findActive(DAY_3)

            assertEquals(listOf(running), result)
        }

        @Test
        fun `should return an empty list if no announcement is shown`() {
            create(text = "Будущее", shownFrom = DAY_3, shownUntil = DAY_4)

            assertEquals(emptyList<Announcement>(), announcements.findActive(DAY_2))
        }
    }

    private fun create(text: String, shownFrom: Instant, shownUntil: Instant, link: String? = null): Announcement {
        val result = announcements.create(
            text = text,
            link = link,
            kind = AnnouncementKind.Info,
            shownFrom = shownFrom,
            shownUntil = shownUntil,
        )
        return checkNotNull((result as? AnnouncementSave.Saved)?.announcement) { "Announcement '$text' is not stored: $result" }
    }

    companion object {
        private val DAY_1 = Instant.parse("2026-01-01T00:00:00Z")
        private val DAY_2 = Instant.parse("2026-01-02T00:00:00Z")
        private val DAY_3 = Instant.parse("2026-01-03T00:00:00Z")
        private val DAY_4 = Instant.parse("2026-01-04T00:00:00Z")

        private const val LINK_PREFIX = "https://example.com/"
        private const val OLD_LINK = "https://example.com/old"

        /** Valid link of [count] code points: the prefix followed by a path of supplementary characters. */
        private fun linkOfCodePoints(count: Int): String = LINK_PREFIX + "😀".repeat(count - LINK_PREFIX.length)

        @JvmStatic
        fun finishedMoments(): List<Instant> = listOf(DAY_2, DAY_3)

        @JvmStatic
        fun absentLinks(): List<String?> = listOf(null, "", " \t ")

        @JvmStatic
        fun refusals(): List<Arguments> = listOf(
            Arguments.of(" \t ", "javascript:alert(1)", DAY_2, DAY_1, AnnouncementRejection.BlankText),
            Arguments.of("😀".repeat(301), "javascript:alert(1)", DAY_2, DAY_1, AnnouncementRejection.TextTooLong),
            Arguments.of("Текст", "javascript:alert(1)", DAY_2, DAY_1, AnnouncementRejection.InvalidLink),
            Arguments.of("Текст", "ftp://example.com/file", DAY_1, DAY_2, AnnouncementRejection.InvalidLink),
            Arguments.of("Текст", "/news/1", DAY_1, DAY_2, AnnouncementRejection.InvalidLink),
            Arguments.of("Текст", "example.com", DAY_1, DAY_2, AnnouncementRejection.InvalidLink),
            Arguments.of("Текст", "https://user@example.com", DAY_1, DAY_2, AnnouncementRejection.InvalidLink),
            Arguments.of("Текст", "http://", DAY_1, DAY_2, AnnouncementRejection.InvalidLink),
            Arguments.of("Текст", "https:///path", DAY_1, DAY_2, AnnouncementRejection.InvalidLink),
            Arguments.of("Текст", "https://example.com/a b", DAY_1, DAY_2, AnnouncementRejection.InvalidLink),
            Arguments.of("Текст", linkOfCodePoints(2049), DAY_2, DAY_1, AnnouncementRejection.LinkTooLong),
            Arguments.of("Текст", null, DAY_1, DAY_1, AnnouncementRejection.EmptyPeriod),
            Arguments.of("Текст", "https://example.com", DAY_2, DAY_1, AnnouncementRejection.EmptyPeriod),
        )
    }
}
