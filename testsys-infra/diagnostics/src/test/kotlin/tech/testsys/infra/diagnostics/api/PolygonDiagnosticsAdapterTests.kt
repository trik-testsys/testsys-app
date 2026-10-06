package tech.testsys.infra.diagnostics.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import tech.testsys.domain.builder.api.test
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.model.task.*
import tech.testsys.infra.diagnostics.internal.DiagnosticsProperties
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@OptIn(InternalDiagnosticsApi::class)
class PolygonDiagnosticsAdapterTests {
    private val adapter = PolygonDiagnosticsAdapter(
        DiagnosticsProperties(maxTimeLimitMillis = 600_000, scorePrefix = "Набрано баллов:"),
    )

    @ParameterizedTest
    @CsvSource("0,false", "600000,false", "600001,true", "-1,true")
    fun `should enforce the inclusive configured time interval`(limit: Long, hasError: Boolean) {
        val polygon = polygon("<timelimit value='$limit'/><init><message text='Набрано баллов: 42'/></init>")

        val result = adapter.diagnose(polygon)

        assertEquals(TestId(5), result.testId)
        assertEquals(hasError, result.reports.any { it.severity == DiagnosticSeverity.Error })
    }

    @Test
    fun `should report a missing time limit and missing score output independently`() {
        val result = adapter.diagnose(polygon(""))

        assertEquals(listOf(DiagnosticData.MissingTimeLimit, DiagnosticData.MissingScoreOutput), result.reports.map { it.data })
        assertEquals(listOf(DiagnosticSeverity.Error, DiagnosticSeverity.Warning), result.reports.map { it.severity })
    }

    @Test
    fun `should report repeated time limits even with equal values`() {
        val result = adapter.diagnose(polygon("<timelimit value='1'/><timelimit value='1'/>"))

        assertEquals(DiagnosticData.MultipleTimeLimits(2), result.reports.first().data)
    }

    @Test
    fun `should preserve distinct locations of identical invalid references`() {
        val xml = "<timelimit value='1'/><init><drop id='missing'/><drop id='missing'/></init>"

        val result = adapter.diagnose(polygon(xml))

        val locations = result.reports.filter { it.data is DiagnosticData.InvalidEventId }.mapNotNull { it.location }
        assertEquals(listOf(1, 2), locations.map { it.path.last().index })
        assertEquals(listOf("id", "id"), locations.map { it.attribute })
        assertEquals(listOf("root", "constraints", "init", "drop"), locations.first().path.map { it.tag })
    }

    @Test
    fun `should resolve condition and trigger event references through recursively nested conditions`() {
        val xml = """
            <timelimit value="1"/>
            <event id="known">
                <conditions glue="and">
                    <conditions glue="or"><settedUp id="known"/><not><dropped id="known"/></not></conditions>
                </conditions>
                <triggers><setUp id="known"/><drop id="known"/><message text="Набрано баллов: 42"/></triggers>
            </event>
        """.trimIndent()

        val result = adapter.diagnose(polygon(xml))

        assertEquals(emptyList<DiagnosticReport>(), result.reports)
    }

    @Test
    fun `should allow an anonymous event with references to a named sibling`() {
        val xml = """
            <timelimit value="1"/>
            <event id="named"><condition><true/></condition><trigger><success/></trigger></event>
            <event><condition><settedUp id="named"/></condition><trigger><drop id="named"/></trigger></event>
        """.trimIndent()

        val result = adapter.diagnose(polygon(xml))

        assertEquals(listOf(DiagnosticData.MissingScoreOutput), result.reports.map { it.data })
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "<future><drop id='missing'/><message text='Набрано баллов: 42'/><timelimit value='bad'/></future>",
            "<init><setState><drop id='missing'/><message text='Набрано баллов: 42'/></setState></init>",
            "<event><condition><using><dropped id='missing'/></using></condition><trigger><success/></trigger></event>",
        ],
    )
    fun `should emit Info and skip the contents of unknown constructions`(unknown: String) {
        val result = adapter.diagnose(polygon("<timelimit value='1'/>$unknown"))

        assertEquals(listOf(DiagnosticSeverity.Info, DiagnosticSeverity.Warning), result.reports.map { it.severity })
        assertEquals(DiagnosticData.MissingScoreOutput, result.reports.last().data)
        assertTrue(result.reports.first().data is DiagnosticData.UnknownElement)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "<root><world/><constraints><timelimit value='1'></constraints></root>",
            "<root><world/><constraints><timelimit value='1'/></constraints>",
            "<root/><root/>",
            "<root><world duplicate='1' duplicate='2'/><constraints/></root>",
            "<root><world/><constraints><init><message text='&unknown;'/></init></constraints></root>",
        ],
    )
    fun `should return only parsing Error for malformed XML without semantic diagnostics`(xml: String) {
        val result = adapter.diagnose(polygonXml(xml))

        assertEquals(1, result.reports.size)
        assertEquals(DiagnosticSeverity.Error, result.reports.single().severity)
        assertTrue(result.reports.single().data is DiagnosticData.MalformedXml)
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["x", "9223372036854775808", "1.5"],
    )
    fun `should reject malformed numeric time limits at their value attribute`(value: String) {
        val result = adapter.diagnose(polygon("<timelimit value='$value'/>"))

        assertEquals(1, result.reports.size)
        assertTrue(result.reports.single().data is DiagnosticData.InvalidAttributeValue)
        assertEquals("value", result.reports.single().location?.attribute)
    }

    @Test
    fun `should locate a missing attribute at its existing parent`() {
        val result = adapter.diagnose(polygon("<timelimit/>"))

        assertEquals(DiagnosticData.MissingAttribute("value"), result.reports.single().data)
        assertEquals("timelimit", result.reports.single().location?.path?.last()?.tag)
        assertNull(result.reports.single().location?.attribute)
    }

    @Test
    fun `should reject constraints nested in world instead of the root sibling`() {
        val result = adapter.diagnose(polygonXml("<root><world><constraints><timelimit value='1'/></constraints></world></root>"))

        assertEquals(DiagnosticData.MissingChild("constraints"), result.reports.single().data)
        assertEquals("root", result.reports.single().location?.path?.last()?.tag)
    }

    @Test
    fun `should distinguish score messages from log text and unknown wrappers`() {
        val result = adapter.diagnose(polygon("<timelimit value='1'/><init><log text='Набрано баллов: 42'/></init>"))

        assertEquals(listOf(DiagnosticData.MissingScoreOutput), result.reports.map { it.data })
    }

    @Test
    fun `should keep analysis results independent across repeated and parallel calls`() {
        val valid = polygon("<timelimit value='1'/><init><message text='Набрано баллов: 42'/></init>")
        val invalid = polygon("<timelimit value='-1'/>")
        val executor = Executors.newFixedThreadPool(2)

        val results = executor.use {
            it.invokeAll(
                listOf(
                    Callable { adapter.diagnose(valid) },
                    Callable { adapter.diagnose(invalid) },
                    Callable { adapter.diagnose(valid) },
                ),
            ).map { future -> future.get() }
        }

        assertEquals(emptyList<DiagnosticReport>(), results[0].reports)
        assertEquals(DiagnosticData.NegativeTimeLimit(-1), results[1].reports.first().data)
        assertEquals(emptyList<DiagnosticReport>(), results[2].reports)
    }

    @Test
    fun `should use default configuration in the registered application context`() {
        val context = AnnotationConfigApplicationContext(DiagnosticsConfiguration::class.java)

        val result = context.use {
            it.getBean(PolygonDiagnostics::class.java).diagnose(
                polygon("<timelimit value='600000'/><init><message text='Набрано баллов: 42'/></init>"),
            )
        }

        assertEquals(emptyList<DiagnosticReport>(), result.reports)
    }

    @Test
    fun `should override the time limit with application configuration`() {
        val context = AnnotationConfigApplicationContext()
        context.environment.propertySources.addFirst(MapPropertySource("test", mapOf("testsys.diagnostics.max-time-limit-millis" to 10)))
        context.register(DiagnosticsConfiguration::class.java)
        context.refresh()

        val result = context.use {
            it.getBean(PolygonDiagnostics::class.java).diagnose(polygon("<timelimit value='11'/>"))
        }

        assertEquals(DiagnosticData.ExcessiveTimeLimit(value = 11, maximum = 10), result.reports.first().data)
    }

    @ParameterizedTest
    @CsvSource("Score: 42,false", "Набрано баллов: 42,true", "score: 42,true")
    fun `should use the application score prefix instead of the default marker`(message: String, hasWarning: Boolean) {
        val context = AnnotationConfigApplicationContext()
        context.environment.propertySources.addFirst(
            MapPropertySource("test", mapOf("testsys.diagnostics.score-prefix" to "Score:")),
        )
        context.register(DiagnosticsConfiguration::class.java)
        context.refresh()

        val result = context.use {
            it.getBean(PolygonDiagnostics::class.java).diagnose(
                polygon("<timelimit value='1'/><init><message text='$message'/></init>"),
            )
        }

        assertEquals(
            hasWarning,
            result.reports.any { it.data == DiagnosticData.MissingScoreOutput },
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\t"])
    fun `should reject a blank configured score prefix`(prefix: String) {
        assertThrows(IllegalArgumentException::class.java) {
            DiagnosticsProperties(maxTimeLimitMillis = 600_000, scorePrefix = prefix)
        }
    }

    private fun polygon(constraints: String): tech.testsys.domain.model.task.Test =
        polygonXml("<root><world/><constraints>$constraints</constraints></root>")

    private fun polygonXml(xml: String): tech.testsys.domain.model.task.Test = test {
        id = 5
        createdAt = Instant.EPOCH
        data = testData {
            name = "Polygon"
            description = ""
            versionBucket = VersionBucket(UUID(0, 1))
            file("world.xml", xml.toByteArray())
        }
    }
}
