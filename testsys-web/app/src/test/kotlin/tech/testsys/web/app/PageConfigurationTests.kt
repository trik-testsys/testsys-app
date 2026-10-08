package tech.testsys.web.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class PageConfigurationTests {
    private val forbidden = Regex(
        listOf(
            "com\\.vaadin\\.flow\\.(component\\.(?!(?:UI|Component)\\b|dependency\\.|page\\.)|dom)",
            "\\belement\\s*\\.|getStyle\\s*\\(|\\.element\\b|\\.style\\b|\\.class(?:Names?|List)\\b|executeJs\\s*\\(",
            "(?:addClassNames?|setClassName|setAttribute|getElement|custom)\\s*\\(|RawVaadin",
        ).joinToString("|"),
    )

    @Test
    fun `should configure application pages without raw markup or styling`() {
        val roots = listOf(Path.of("src/main/kotlin"))

        val violations = roots.flatMap { root ->
            Files.walk(root).use { paths ->
                paths.filter { path -> path.toString().endsWith(".kt") }.toList().flatMap { file ->
                    file.readText().lines().mapIndexedNotNull { index, line ->
                        if (forbidden.containsMatchIn(line)) "$file:${index + 1}: $line" else null
                    }
                }
            }
        }

        assertEquals(emptyList<String>(), violations)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "page.element",
            "element.style",
            "element.setProperty(\"innerHTML\", \"value\")",
            "element.getStyle().set(\"color\", \"red\")",
            "view.classNames",
            "getElement()",
            "executeJs()",
            "custom(component)",
            "import com.vaadin.flow.component.button.Button",
            "import com.vaadin.flow.dom.Element",
        ],
    )
    fun `should detect raw configuration in a source fragment`(source: String) {
        assertTrue(forbidden.containsMatchIn(source))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "import com.vaadin.flow.component.UI",
            "import com.vaadin.flow.component.Component",
            "import com.vaadin.flow.component.page.Push",
            "import com.vaadin.flow.data.binder.Binder",
            "import com.vaadin.flow.signals.Signal",
        ],
    )
    fun `should allow supported framework integration in a source fragment`(source: String) {
        assertFalse(forbidden.containsMatchIn(source))
    }
}
