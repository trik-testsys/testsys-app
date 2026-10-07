package tech.testsys.web.components.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.testTexts
import java.util.Locale

class AvatarTests : MockVaadinTests() {
    private val people = listOf(AvatarData("One"), AvatarData("Two"), AvatarData("Three"))

    @Test
    fun `should show initials and keep the full name as the accessible name`() {
        val root = buildTestContent { avatar(AvatarData("Иван Петров")) }

        val avatar = root.find("ts-avatar")
        assertEquals("ИП", avatar.element.text)
        assertEquals("Иван Петров", avatar.element.getAttribute("aria-label"))
        assertEquals("img", avatar.element.getAttribute("role"))
    }

    @Test
    fun `should update explicit initials and square mode through the handle`() {
        lateinit var handle: DataHandle<AvatarData>
        val root = buildTestContent { handle = avatar(AvatarData("Иван Петров")) }

        handle.data = AvatarData("Organization", "TS", isSquare = true)

        val avatar = root.find("ts-avatar")
        assertEquals("TS", avatar.element.text)
        assertTrue("ts-avatar--square" in avatar.element.classList)
        assertEquals("Organization", avatar.element.getAttribute("aria-label"))
    }

    @Test
    fun `should derive initials with the casing of the locale`() {
        assertEquals("İİ", avatarInitials("istanbul izmir", Locale.forLanguageTag("tr")))
    }

    @Test
    fun `should share compact geometry with the overflow avatar`() {
        val root = buildTestContent {
            ContentScope(container, texts, Placement.Head, gridColumns).avatarGroup(people, maxVisible = 1)
        }

        val avatars = root.findAll("ts-avatar")
        assertEquals(2, avatars.size)
        assertTrue(avatars.all { avatar -> "ts-avatar--compact" in avatar.element.classList })
        assertEquals(testTexts.components.avatarOverflow(2), avatars.last().element.getAttribute("aria-label"))
    }

    @Test
    fun `should update the visible group through the handle`() {
        lateinit var handle: DataHandle<List<AvatarData>>
        val root = buildTestContent {
            handle = ContentScope(container, texts, Placement.Head, gridColumns).avatarGroup(people, maxVisible = 1)
        }

        handle.data = people.take(1)

        assertEquals(1, root.findAll("ts-avatar").size)
    }
}
