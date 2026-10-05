package tech.testsys.web.components.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.display.field
import tech.testsys.web.components.find
import tech.testsys.web.components.overlay.popover

class LeaderboardTests : MockVaadinTests() {
    @Test
    fun `should inherit a narrow block grid and preserve row and column semantics`() {
        val data = LeaderboardData(
            label = "Results",
            placeLabel = "Rank",
            identityLabel = "Participant",
            placeSize = 1,
            identitySize = 4,
            columns = listOf(LeaderboardColumn("score", "Score", isMetric = true, size = null)),
            rows = listOf(LeaderboardRow("user", "1", "Name", cells = mapOf("score" to LeaderboardCell("50")))),
        )
        val root = buildTestPage {
            row { slot(size = 8) { row { block { leaderboard(data) } } } }
        }.find("ts-leaderboard")

        val table = root.element.getChild(0)
        assertEquals("100.0%", table.style.get("--ts-table-width"))
        assertEquals("8", table.style.get("--ts-table-used"))
        assertEquals("Results", table.getAttribute("aria-label"))
        assertEquals("37.5%", table.getChild(0).getChild(2).style.get("width"))
        assertEquals("row", table.getChild(2).getChild(0).getChild(1).getAttribute("scope"))
    }

    @Test
    fun `should reject an overflowing leaderboard grid`() {
        val data = LeaderboardData(
            label = "Results",
            placeLabel = "Rank",
            identityLabel = "Participant",
            columns = listOf(LeaderboardColumn("score", "Score", size = 1)),
            rows = emptyList(),
            gridColumns = 8,
        )

        assertThrows<IllegalStateException> { buildTestPage { block { leaderboard(data) } } }
    }

    @Test
    fun `should inherit four fractions through a field value and nested groups`() {
        val data = smallBoard(totalSize = 4)

        val root = buildTestPage {
            block {
                row {
                    field(
                        "Value",
                        labelSize = 1,
                        size = 4,
                    ) { horizontal { vertical { leaderboard(data) } } }
                }
            }
        }

        assertEquals("100.0%", root.find("ts-leaderboard").element.getChild(0).style.get("--ts-table-width"))
        assertEquals("4", root.find("ts-leaderboard").element.getChild(0).style.get("--ts-table-used"))
    }

    @Test
    fun `should reject six fractions in a field value assigned four fractions`() {
        val data = smallBoard(totalSize = 6)

        assertThrows<IllegalStateException> {
            buildTestPage { block { row { field("Value", labelSize = 1, size = 4) { leaderboard(data) } } } }
        }
    }

    @Test
    fun `should inherit a narrow block footer grid`() {
        val data = smallBoard(totalSize = 4)

        val root = buildTestPage { row { slot(size = 4) { row { block { footer { leaderboard(data) } } } } } }

        assertEquals("100.0%", root.find("ts-leaderboard").element.getChild(0).style.get("--ts-table-width"))
    }

    @Test
    fun `should inherit a narrow block action grid`() {
        val data = smallBoard(totalSize = 4)

        val root = buildTestPage { row { slot(size = 4) { row { block { actions { leaderboard(data) } } } } } }

        assertEquals("100.0%", root.find("ts-leaderboard").element.getChild(0).style.get("--ts-table-width"))
    }

    @Test
    fun `should pass a remaining cell capacity after selection and menu reservations`() {
        val data = smallBoard(totalSize = 4)

        val root = buildTestPage {
            block {
                table(key = { id: Int -> id }, selectable = true, gridColumns = 8, fetch = { Page(listOf(1), 1) }) {
                    textColumn("ID", size = 2) { "1" }
                    column("Results") { leaderboard(data) }
                    menuColumn { item("Open") {} }
                }
            }
        }

        assertEquals("100.0%", root.find("ts-leaderboard").element.getChild(0).style.get("--ts-table-width"))
        assertEquals("4", root.find("ts-leaderboard").element.getChild(0).style.get("--ts-table-used"))
    }

    @Test
    fun `should show the table load error if nested data exceed a remaining cell capacity`() {
        val data = smallBoard(totalSize = 6)

        val root = buildTestPage {
            block {
                table(key = { id: Int -> id }, selectable = true, gridColumns = 6, fetch = { Page(listOf(1), 1) }) {
                    column("Results") { leaderboard(data) }
                    menuColumn { item("Open") {} }
                }
            }
        }

        assertTrue(root.find("ts-empty--error").isVisible)
    }

    @Test
    fun `should inherit the assigned popup capacity through its content wrapper`() {
        val data = smallBoard(totalSize = 4)

        val root = buildTestPage { block { row { popover("Results", size = 4) { leaderboard(data) } } } }

        assertEquals("100.0%", root.find("ts-leaderboard").element.getChild(0).style.get("--ts-table-width"))
    }

    private fun smallBoard(totalSize: Int): LeaderboardData = LeaderboardData(
        label = "Results",
        placeLabel = "Rank",
        identityLabel = "Name",
        placeSize = 1,
        identitySize = totalSize - 2,
        columns = listOf(LeaderboardColumn("score", "Score", size = 1)),
        rows = emptyList(),
    )
}
