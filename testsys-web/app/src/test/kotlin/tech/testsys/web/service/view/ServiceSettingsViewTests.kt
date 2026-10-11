package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.textfield.TextField
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingKey
import tech.testsys.infra.settings.api.SettingsChangeResult
import tech.testsys.infra.settings.api.SettingsOverview
import tech.testsys.infra.settings.api.SettingsPersistence
import tech.testsys.infra.settings.api.SettingsService
import tech.testsys.infra.settings.api.SettingsValues
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.error.ServiceAccessDeniedView
import tech.testsys.web.service.security.ServiceSignIn

class ServiceSettingsViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var settings: SettingsService

    @Autowired
    private lateinit var runtime: RuntimeSettings

    @Autowired
    private lateinit var signIn: ServiceSignIn

    @BeforeEach
    fun prepareSettings() {
        clearMocks(settings)
        every { settings.overview() } returns SettingsOverview(0, runtime.snapshot(), SettingsPersistence.Known)
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceSettingsView::class.java)
    }

    @Test
    fun `should deny settings to a main cabinet principal without service authority`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            CabinetPrincipal(userId = 1, kind = UserKind.MULTIPLE_ROLE),
            null,
            listOf(SimpleGrantedAuthority("ROLE_MULTIPLE_ROLE")),
        )

        UI.getCurrent().navigate(ServiceSettingsView::class.java)

        assertEquals(ServiceAccessDeniedView::class.java, currentView)
    }

    @Test
    fun `should show catalog and readonly community without mail transport settings`() {
        assertEquals(17, _find<TextField>().size)
        assertTrue(_find<TextField>().all { field -> field.isReadOnly })
        assertFalse(_find<Div>().any { text -> text.text.contains("SMTP") })
    }

    @Test
    fun `should separate complete parameters under section headings without active value rows`() {
        assertEquals(
            listOf("Соревнования", "Подтверждение почты", "Приглашения", "Проверка", "Диагностика", "Очистка файлов"),
            _find<H3>().filter { heading -> heading.hasClassName("ts-block__section-title") }.map { heading -> heading.text },
        )
        val separators = _find<Hr>()
        assertEquals(16, separators.size)
        val body = _find<Div>().single { div -> div.hasClassName("ts-block__body") }
        val children = body.children.toList()
        assertTrue(
            separators.all { separator ->
                children[children.indexOf(separator) - 1]._find<TextField>().size == 1
            },
        )
        assertFalse(_find<Div>().any { div -> div.text.startsWith("Действует:") })
        assertEquals("hr", children[children.lastIndex - 1].element.tag)
        assertTrue(children.last().element.textRecursively.contains("Публичное Сообщество (ID)"))
    }

    @ParameterizedTest
    @EnumSource(SettingKey::class)
    fun `should show property path and enabled named help for each catalog setting`(key: SettingKey) {
        val field = _find<TextField>().single { field -> field.helperText == key.property }
        val help = field.parent.orElseThrow().parent.orElseThrow().parent.orElseThrow()._find<Button>().single()

        assertEquals(key.property, field.helperText)
        assertEquals(help.element.getAttribute("aria-label"), help.tooltip.text)
        assertTrue(help.isEnabled)
        val paragraphs = help.tooltip.text.split("\n\n")
        assertEquals(3, paragraphs.size)
        assertTrue(paragraphs.all { paragraph -> paragraph.isNotBlank() })
        assertTrue(paragraphs.last().startsWith("Изменения вступят в силу "))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "Лимит Участников | Неотрицательное целое число типа Kotlin Int | при последующих добавлениях Участников",
            "Попытки подтверждения | Положительное целое число типа Kotlin Int | при создании или перезаписи запроса",
            "Срок приглашения в Класс | Положительная длительность ISO-8601, например PT5M | " +
                "при создании, замене или продлении Кода-приглашения",
            "Видеозапись | true — включена, false — выключена | при приёме новой проверки",
            "Предел времени Диагностики, мс | Неотрицательное целое число типа Kotlin Long, в миллисекундах | при следующей Диагностике",
            "Префикс баллов | Текст не должен быть пустым или состоять только из пробелов; пробелы сохраняются | при следующей Диагностике",
            "Размер пакета очистки | Положительное целое число типа Kotlin Int | при следующем явном проходе очистки",
        ],
    )
    fun `should explain input format and application boundary in help`(label: String, format: String, boundary: String) {
        val field = _find<TextField>().single { field -> field.ariaLabel.orElseThrow() == label }
        val help = field.parent.orElseThrow().parent.orElseThrow().parent.orElseThrow()._find<Button>().single()

        assertEquals("$format.", help.tooltip.text.split("\n\n")[1])
        assertTrue(help.tooltip.text.split("\n\n").last().startsWith("Изменения вступят в силу $boundary"))
    }

    @Test
    fun `should restore values when editing is cancelled`() {
        _get<Button> { text = "Изменить" }._click()
        val field = _find<TextField>().first()
        field.value = "2"

        _get<Button> { text = "Отменить" }._click()

        assertEquals("100", field.value)
        assertTrue(field.isReadOnly)
        verify(exactly = 0) { settings.change(any(), any()) }
    }

    @Test
    fun `should submit only changed values with expected revision and show success after apply`() {
        val overview = settings.overview()
        every { settings.change(0, mapOf(SettingKey.MaxParticipants to "2")) } returns SettingsChangeResult.Applied(overview)
        _get<Button> { text = "Изменить" }._click()
        _find<TextField>().first().value = "2"

        _get<Button> { text = "Сохранить" }._click()

        verify(exactly = 1) { settings.change(0, mapOf(SettingKey.MaxParticipants to "2")) }
        assertTrue(_find<TextField>().all { field -> field.isReadOnly })
    }

    @Test
    fun `should keep stale form values until explicit reload`() {
        every { settings.change(any(), any()) } returns SettingsChangeResult.Stale
        _get<Button> { text = "Изменить" }._click()
        val field = _find<TextField>().first()
        field.value = "2"

        _get<Button> { text = "Сохранить" }._click()

        assertEquals("2", field.value)
        assertFalse(field.isReadOnly)
    }

    @Test
    fun `should mark rejected field and keep editing after validation failure`() {
        every { settings.change(any(), any()) } returns SettingsChangeResult.Invalid(mapOf(SettingKey.MaxParticipants to "range"))
        _get<Button> { text = "Изменить" }._click()
        val field = _find<TextField>().first()
        field.value = "-1"

        _get<Button> { text = "Сохранить" }._click()

        assertTrue(field.isInvalid)
        assertFalse(field.isReadOnly)
    }

    @Test
    fun `should preserve unsaved values and editing after a confirmed apply failure`() {
        every { settings.change(any(), any()) } returns SettingsChangeResult.Failed
        _get<Button> { text = "Изменить" }._click()
        val field = _find<TextField>().first()
        field.value = "2"

        _get<Button> { text = "Сохранить" }._click()

        assertEquals("2", field.value)
        assertFalse(field.isReadOnly)
    }

    @Test
    fun `should show saved transport value and active value while restart is pending`() {
        val values = SettingKey.entries.associateWith { key -> runtime.snapshot().values[key] }.toMutableMap()
        values[SettingKey.MaxMessageBytes] = "1024"
        val pending = runtime.snapshot().copy(values = SettingsValues(values))
        every { settings.overview() } returns SettingsOverview(1, pending, SettingsPersistence.Known)

        UI.getCurrent().navigate(ServiceSettingsView::class.java)

        assertEquals("1024", _find<TextField>().single { field -> field.helperText == SettingKey.MaxMessageBytes.property }.value)
        assertTrue(
            _find<Div>().any { text ->
                text.text == "Размер входящего сообщения: действующий предел — 400000000 байт, " +
                    "сохранённый — 1024 байт. Для применения требуется перезапуск."
            },
        )
        val field = _find<TextField>().single { field -> field.helperText == SettingKey.MaxMessageBytes.property }
        val help = field.parent.orElseThrow().parent.orElseThrow().parent.orElseThrow()._find<Button>().single()
        assertTrue(help.tooltip.text.contains("Изменения вступят в силу после перезапуска приложения"))
        assertTrue(help.tooltip.text.contains("Действующий предел — 400000000 байт"))
    }

    @Test
    fun `should block editing when saved state is uncertain`() {
        every { settings.overview() } returns SettingsOverview(0, runtime.snapshot(), SettingsPersistence.Uncertain)

        UI.getCurrent().navigate(ServiceSettingsView::class.java)

        assertTrue(_find<Button> { text = "Изменить" }.isEmpty())
        assertTrue(_find<TextField>().all { field -> field.isReadOnly })
    }
}
