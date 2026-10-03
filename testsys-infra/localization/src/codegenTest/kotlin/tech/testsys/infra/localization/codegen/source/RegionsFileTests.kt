package tech.testsys.infra.localization.codegen.source

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegionsFileTests {

    @Test
    fun `should reject an exclamation mark comment and end the KDoc block`() {
        val errors = mutableListOf<String>()

        val regions = RegionsFile.parse("# Russia.\n! other comment\nRU=ru-RU", errors)

        assertEquals(listOf(Triple("RU", "ru-RU", null)), regions.map { Triple(it.id, it.languageTag, it.comment) })
        assertEquals(listOf("regions.properties:2: Comments must start with #"), errors)
    }

    @Test
    fun `should parse regions in order with the comment directly above as KDoc`() {
        val errors = mutableListOf<String>()

        val regions = RegionsFile.parse("# Russia.\nRU=ru-RU\n\n# Ignored.\n\nEN=en-US\n", errors)

        assertEquals(
            listOf(Triple("RU", "ru-RU", "Russia."), Triple("EN", "en-US", null)),
            regions.map { Triple(it.id, it.languageTag, it.comment) },
        )
        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `should join consecutive comment lines into the KDoc`() {
        val errors = mutableListOf<String>()

        val regions = RegionsFile.parse("# Russia:\n# Russian language.\nRU=ru-RU\n", errors)

        assertEquals(listOf("Russia: Russian language."), regions.map { it.comment })
    }

    @Test
    fun `should parse a regions file with CRLF line breaks`() {
        val errors = mutableListOf<String>()

        val regions = RegionsFile.parse("# Russia.\r\nRU=ru-RU\r\nEN=en-US\r\n", errors)

        assertEquals(
            listOf(Triple("RU", "ru-RU", "Russia."), Triple("EN", "en-US", null)),
            regions.map { Triple(it.id, it.languageTag, it.comment) },
        )
        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `should report a region id that is not two capital letters`() {
        val errors = mutableListOf<String>()

        RegionsFile.parse("RUS=ru-RU\nRU=ru-RU", errors)

        assertEquals(listOf("regions.properties:1: region id 'RUS' must match [A-Z]{2}"), errors)
    }

    @Test
    fun `should report an invalid language tag`() {
        val errors = mutableListOf<String>()

        RegionsFile.parse("RU=ru_RU", errors)

        assertEquals(
            listOf(
                "regions.properties:1: 'ru_RU' of region 'RU' is not a valid BCP 47 language tag",
                "regions.properties:1: no regions are declared",
            ),
            errors,
        )
    }

    @Test
    fun `should report a region declared twice`() {
        val errors = mutableListOf<String>()

        RegionsFile.parse("RU=ru-RU\nRU=ru-RU", errors)

        assertEquals(listOf("regions.properties:2: region 'RU' is declared twice"), errors)
    }

    @Test
    fun `should report a line that is not an entry`() {
        val errors = mutableListOf<String>()

        RegionsFile.parse("RU=ru-RU\nEN", errors)

        assertEquals(listOf("regions.properties:2: expected '<REGION>=<BCP 47 language tag>'"), errors)
    }
}
