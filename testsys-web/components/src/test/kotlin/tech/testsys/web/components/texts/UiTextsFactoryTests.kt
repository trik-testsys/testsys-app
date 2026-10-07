package tech.testsys.web.components.texts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.DayOfWeek

class UiTextsFactoryTests {
    private val texts = buildUiTexts()

    @Test
    internal fun `should localize the filter disclosure and its actions`() {
        assertEquals("Настроить фильтры", texts.tableFilters.title)
        assertEquals("Применить", texts.tableFilters.apply)
        assertEquals("Сбросить", texts.tableFilters.reset)
    }

    @Test
    internal fun `should format footer years without grouping and name its navigation`() {
        assertEquals("2037", texts.footer.year(2037))
        assertEquals("Ссылки футтера", texts.footer.links)
    }

    @Test
    fun `should localize header accessible names with application labels and counts`() {
        assertEquals("Поиск по разделам", texts.header.search)
        assertEquals("Уведомления: непрочитанных 2", texts.header.unreadCount(2))
        assertEquals("Тур опубликован — не прочитано", texts.header.unreadItem("Тур опубликован"))
        assertEquals("Меню пользователя: Анна", texts.header.userMenu("Анна"))
        assertEquals("Прочитать все", texts.header.readAll)
        assertEquals("Получено 3 новых уведомления", texts.header.arrivalCount(3))
        assertEquals("Получено 5 новых уведомлений", texts.header.arrivalCount(5))
        assertEquals("Открыть", texts.header.arrivalOpen)
        assertEquals("Закрыть уведомление", texts.header.arrivalClose)
    }

    @Test
    fun `should localize compound date names and required range instruction`() {
        assertEquals("дата", texts.dateFields.date)
        assertEquals("время", texts.dateFields.time)
        assertEquals("Период: с", texts.dateFields.rangeFrom("Период"))
        assertEquals("Период: до", texts.dateFields.rangeTo("Период"))
        assertEquals("С", texts.dateFields.rangeFromPrefix)
        assertEquals("До", texts.dateFields.rangeToPrefix)
        assertEquals("Укажите хотя бы одну границу периода", texts.dateFields.rangeRequired)
    }

    @Test
    fun `should localize not found screen and brand in page title`() {
        assertEquals("Страница не найдена", texts.notFound.title)
        assertEquals("Проверьте адрес или вернитесь на предыдущую страницу.", texts.notFound.description)
        assertEquals("Вернуться", texts.notFound.back)
        assertEquals("Страница не найдена — TestSys", texts.notFound.pageTitle)
    }

    @Test
    fun `should take calendar names and first weekday from the standard locale data`() {
        assertEquals(12, texts.calendar.monthNames.size)
        assertEquals("Январь", texts.calendar.monthNames.first())
        assertEquals(7, texts.calendar.weekdays.size)
        assertEquals(7, texts.calendar.weekdaysShort.size)
        assertEquals("воскресенье", texts.calendar.weekdays.first())
        assertEquals("вс", texts.calendar.weekdaysShort.first())
        assertEquals("пн", texts.calendar.weekdaysShort[1])
        assertEquals(DayOfWeek.MONDAY, texts.calendar.firstDayOfWeek)
        assertEquals("dd.MM.yyyy", texts.calendar.dateFormat)
    }

    @Test
    fun `should take built-in texts from the Russian text factory`() {
        assertEquals("Войти", texts.signIn)
        assertEquals("Изменить", texts.editing.start)
        assertEquals("Нет данных", texts.table.empty)
        assertEquals("1–20 из 1 412", texts.table.range(1, 20, 1412))
        assertEquals("Введите «Весенний кубок», чтобы подтвердить", texts.dialog.typeToConfirm("Весенний кубок"))
        assertEquals("Ничего не найдено", texts.lookup.empty)
    }

    @Test
    fun `should take pagination texts from the Russian text factory`() {
        assertEquals("Перейти на предыдущую страницу", texts.pagination.previous)
        assertEquals("Перейти на следующую страницу", texts.pagination.next)
        assertEquals("Страница 1 412", texts.pagination.page(1412))
    }

    @Test
    fun `should take multi-value lookup texts from the Russian text factory`() {
        assertEquals("Убрать Кубок 1", texts.lookup.remove("Кубок 1"))
        assertEquals("Сбросить", texts.lookup.reset)
        assertEquals("Применить", texts.lookup.apply)
        assertEquals("Выбрано: 1 412", texts.lookup.selectedCount(1412))
    }

    @Test
    fun `should take navigation, menu and load failure texts from the Russian text factory`() {
        assertEquals("Навигационная цепочка", texts.navigation.breadcrumbs)
        assertEquals("Разделы", texts.navigation.sections)
        assertEquals("Действия", texts.menu.actions)
        assertEquals("Не удалось загрузить", texts.load.failed)
        assertEquals("Попробуйте ещё раз", texts.load.failedHint)
        assertEquals("Повторить", texts.load.retry)
    }

    @ParameterizedTest
    @CsvSource(
        "0, Получено 0 новых уведомлений",
        "1, Получено 1 новое уведомление",
        "2, Получено 2 новых уведомления",
        "5, Получено 5 новых уведомлений",
        "11, Получено 11 новых уведомлений",
        "21, Получено 21 новое уведомление",
        "22, Получено 22 новых уведомления",
        "25, Получено 25 новых уведомлений",
    )
    internal fun `should preserve Russian arrival count forms at plural boundaries`(count: Int, expected: String) {
        assertEquals(expected, texts.header.arrivalCount(count))
    }

    @ParameterizedTest
    @CsvSource("1, файла", "2, файлов", "5, файлов", "11, файлов", "21, файла", "22, файлов", "25, файлов")
    internal fun `should preserve Russian upload limit forms`(count: Int, files: String) {
        assertEquals("До $count $files, до 512 Б каждый", texts.components.uploadLimits(count, 512))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "1023|1 023 Б",
            "1024|1 КБ",
            "1536|1,5 КБ",
            "10485760|10 МБ",
            "2147483648|2 ГБ",
        ],
    )
    internal fun `should show upload size limits in binary units`(bytes: Long, size: String) {
        assertEquals("До 1 файла, до $size каждый", texts.components.uploadLimits(1, bytes))
    }

    @Test
    internal fun `should name verb actions of cancelling, returning and the calendar today date`() {
        assertEquals("Отменить", texts.editing.cancel)
        assertEquals("Отменить", texts.dialog.cancel)
        assertEquals("Отменить", texts.calendar.cancel)
        assertEquals("Сегодня", texts.calendar.today)
    }

    @Test
    internal fun `should take transfer status and hidden value texts from the Russian text factory`() {
        assertEquals("Отменено", texts.components.cancelled)
        assertEquals("Убрать файл", texts.components.removeFile)
        assertEquals("Передача приостановлена", texts.components.stalled)
        assertEquals("Ещё 7", texts.components.more(7))
    }

    @Test
    internal fun `should format transferred byte counts beyond the integer range`() {
        assertEquals("Передано: 2 147 483 648 Б", texts.components.transferBytes(2_147_483_648))
    }
}
