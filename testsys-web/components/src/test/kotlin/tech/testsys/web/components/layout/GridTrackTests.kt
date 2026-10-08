package tech.testsys.web.components.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GridTrackTests {
    @Test
    fun `should give every layout grid 24 columns`() {
        assertEquals(24, GRID_COLUMNS)
    }

    @Nested
    inner class TakeTests {
        @Test
        fun `should accept sizes that fill the row exactly`() {
            val track = GridTrack(capacity = 24, owner = "Row")
            track.take(16)

            track.take(8)

            val error = assertThrows<IllegalStateException> { track.takeRest() }
            assertTrue(requireNotNull(error.message).startsWith("Row is full"))
        }

        @Test
        fun `should accept a row that is not full`() {
            val track = GridTrack(capacity = 24, owner = "Row")

            track.take(7)

            assertEquals(17, track.takeRest())
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 25])
        fun `should reject size outside the track capacity`(size: Int) {
            assertThrows<IllegalArgumentException> { GridTrack(capacity = 24, owner = "Row").take(size) }
        }

        @Test
        fun `should report sizes of an overflowing row`() {
            val track = GridTrack(capacity = 24, owner = "Row")
            track.take(16)

            val error = assertThrows<IllegalStateException> { track.take(12) }

            assertTrue(requireNotNull(error.message).contains("16+12 = 28"))
        }
    }

    @Nested
    inner class TakeRestTests {
        @Test
        fun `should take the columns left in the row`() {
            val track = GridTrack(capacity = 24, owner = "Block row")
            track.take(8)

            assertEquals(16, track.takeRest())
        }

        @Test
        fun `should take the whole row if it is empty`() {
            assertEquals(24, GridTrack(capacity = 24, owner = "Block row").takeRest())
        }

        @Test
        fun `should reject an element after the one that took the rest`() {
            val track = GridTrack(capacity = 24, owner = "Block row")
            track.takeRest()

            val error = assertThrows<IllegalStateException> { track.take(1) }

            assertTrue(requireNotNull(error.message).startsWith("Block row is full"))
        }

        @Test
        fun `should reject the rest of a full row`() {
            val track = GridTrack(capacity = 24, owner = "Block row")
            track.take(24)

            val error = assertThrows<IllegalStateException> { track.takeRest() }

            assertTrue(requireNotNull(error.message).startsWith("Block row is full"))
        }
    }
}
