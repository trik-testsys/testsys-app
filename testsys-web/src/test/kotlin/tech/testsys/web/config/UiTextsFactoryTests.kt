package tech.testsys.web.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.DayOfWeek

class UiTextsFactoryTests {
    private val texts = buildUiTexts(SupportedRegion.RU)

    @Test
    fun `should take calendar names and first weekday from ICU`() {
        assertEquals(12, texts.calendar.monthNames.size)
        assertEquals("Январь", texts.calendar.monthNames.first())
        assertEquals(7, texts.calendar.weekdays.size)
        assertEquals(7, texts.calendar.weekdaysShort.size)
        assertEquals(DayOfWeek.MONDAY, texts.calendar.firstDayOfWeek)
        assertEquals("dd.MM.yyyy", texts.calendar.dateFormat)
    }

    @Test
    fun `should take built-in texts from the localization`() {
        assertEquals("Войти", texts.signIn)
        assertEquals("Изменить", texts.editing.start)
        assertEquals("Нет данных", texts.table.empty)
        assertEquals("1–20 из 1 412", texts.table.range(1, 20, 1412))
        assertEquals("Введите «Весенний кубок», чтобы подтвердить", texts.dialog.typeToConfirm("Весенний кубок"))
        assertEquals("Ничего не найдено", texts.lookup.empty)
    }
}
