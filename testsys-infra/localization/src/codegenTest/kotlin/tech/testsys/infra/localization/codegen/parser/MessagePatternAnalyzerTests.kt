package tech.testsys.infra.localization.codegen.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.parser.Placeholder.InstantPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.IntPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.NumberPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.SelectPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.StringPlaceholder

class MessagePatternAnalyzerTests {

    private val analyzer = MessagePatternAnalyzer()

    @Test
    fun `select with nested plural does not leak plural keywords into select variants`() {
        // Regression test: previously the analyzer collected every ARG_SELECTOR
        // part between the outer ARG_START and ARG_LIMIT regardless of nesting,
        // so `one/few/many` from the inner plural ended up as `case` variants.
        val pattern = """
            {case, select,
                nom {{count, plural, one {# кошелёк} few {# кошелька} many {# кошельков} other {# кошелька}}}
                gen {{count, plural, one {# кошелька} few {# кошельков} many {# кошельков} other {# кошельков}}}
                other {#}
            }
        """.trimIndent()

        val placeholders = analyzer.analyze(pattern)

        val case = placeholders["case"]
        assertTrue(case is SelectPlaceholder) { "expected SelectPlaceholder, got $case" }
        case as SelectPlaceholder
        assertEquals(setOf("nom", "gen", "other"), case.selectVariants)

        assertTrue(placeholders["count"] is IntPlaceholder) {
            "expected count to be IntPlaceholder, got ${placeholders["count"]}"
        }
    }

    @Test
    fun `nested select keeps only outer branch labels`() {
        val pattern = """
            {a, select,
                x {{b, select, p {p} q {q} other {o}}}
                y {y}
                other {o}
            }
        """.trimIndent()

        val placeholders = analyzer.analyze(pattern)

        val a = placeholders["a"] as SelectPlaceholder
        val b = placeholders["b"] as SelectPlaceholder
        assertEquals(setOf("x", "y", "other"), a.selectVariants)
        assertEquals(setOf("p", "q", "other"), b.selectVariants)
    }

    @Test
    fun `flat select returns its branch labels`() {
        val placeholders = analyzer.analyze("{role, select, admin {a} user {u} other {o}}")

        val role = placeholders["role"] as SelectPlaceholder
        assertEquals(setOf("admin", "user", "other"), role.selectVariants)
    }

    @Test
    fun `plural argument is reported as Int`() {
        val placeholders = analyzer.analyze("{days, plural, one {# day} other {# days}}")
        assertTrue(placeholders["days"] is IntPlaceholder)
    }

    @Test
    fun `simple placeholder types map to the right kinds`() {
        val placeholders = analyzer.analyze(
            "{name} {n, number} {amount, number, percent} {when, date, short}"
        )
        assertTrue(placeholders["name"] is StringPlaceholder)
        assertTrue(placeholders["n"] is NumberPlaceholder)
        assertTrue(placeholders["amount"] is NumberPlaceholder)
        assertTrue(placeholders["when"] is InstantPlaceholder)
    }

    @Test
    fun `repeated placeholder within one pattern is collapsed`() {
        val placeholders = analyzer.analyze("{name}, again {name}!")
        assertEquals(1, placeholders.size)
        assertTrue(placeholders["name"] is StringPlaceholder)
    }

    @Test
    fun `repeated placeholder with conflicting kinds fails`() {
        assertThrows(IllegalStateException::class.java) {
            analyzer.analyze("{x} and {x, date, short}")
        }
    }
}
