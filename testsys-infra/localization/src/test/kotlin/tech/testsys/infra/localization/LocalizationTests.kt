package tech.testsys.infra.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.infra.localization.PageDeveloper.DiagnosticLineAtSeverity
import tech.testsys.infra.localization.PageDeveloper.DiagnosticLineSeverity
import tech.testsys.infra.localization.PageDeveloper.RefusalNoExerciseLanguage
import tech.testsys.infra.localization.PageDeveloper.ResourceTitleType
import tech.testsys.infra.localization.PageDeveloper.ResourceTypeType
import tech.testsys.infra.localization.PageDeveloper.TaskResourcesUsageValueUsage
import tech.testsys.infra.localization.PageDeveloper.TaskStateChangeTitleState
import tech.testsys.infra.localization.PageDeveloper.TaskStateState
import tech.testsys.infra.localization.PageDeveloper.TestingSubmissionsStatusStatus
import tech.testsys.infra.localization.PageJudge.SolutionLanguageLanguage
import tech.testsys.infra.localization.PageJudge.SolutionStatusStatus
import tech.testsys.infra.localization.UiTransfer.UploadLimitsUnit
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.Instant
import java.time.ZoneId

// The golden test of the product messages; the functions and constructs are shown by ExampleLocalizationTests.
class LocalizationTests {

    private val l = Localization.forRegion(SupportedRegion.RU, ZoneId.of("Europe/Moscow"))

    @Test
    fun `should point to the example set in todo example`() {
        assertEquals("View examples in testsys-infra/localization/src/test/examples/localization", l.todo.example())
    }

    @Nested
    inner class UserTests {

        @Test
        fun `should name the confirmation code in registration confirmation code subject`() {
            assertEquals("Код подтверждения для регистрации в TestSys", l.user.registrationConfirmationCodeSubject())
        }

        @Test
        fun `should greet without nickname and show the code on its own line in registration confirmation code body`() {
            assertEquals(
                "Здравствуйте!\n\n" +
                    "Вы начали регистрацию в TestSys. Чтобы подтвердить адрес электронной почты, введите этот код " +
                    "на странице регистрации:\n\n" +
                    "01234567\n\n" +
                    "Код действует ограниченное время. Если код больше не действует, начните регистрацию заново, " +
                    "и Вам придёт новый код.\n\n" +
                    "Если Вам пришло несколько таких писем, введите код из последнего.\n\n" +
                    "Если Вы не начинали регистрацию в TestSys, ничего делать не нужно. Просто удалите это письмо.",
                l.user.registrationConfirmationCodeBody(confirmationCode = "01234567"),
            )
        }

        @Test
        fun `should render the access code term in registration access code subject`() {
            assertEquals("Ваш Код-доступа к TestSys", l.user.registrationAccessCodeSubject())
        }

        @Test
        fun `should show the case-sensitive access code verbatim in registration access code body`() {
            assertEquals(
                "Здравствуйте, Маша!\n\n" +
                    "Регистрация в TestSys завершена. Ваш Код-доступа:\n\n" +
                    "aB3d-x9Yz-0kLm-P7qR\n\n" +
                    "Чтобы войти в TestSys, введите этот код на странице входа. Больше ничего вводить не нужно.\n\n" +
                    "Вводите код точно так, как он написан здесь, вместе с дефисами. Большие и маленькие буквы " +
                    "различаются.\n\n" +
                    "Сохраните это письмо или запишите код. Никому его не сообщайте: тот, кто знает код, может войти " +
                    "в TestSys под Вашим именем.",
                l.user.registrationAccessCodeBody(name = "Маша", accessCode = "aB3d-x9Yz-0kLm-P7qR"),
            )
        }

        @Test
        fun `should name the confirmation code in email change confirmation code subject`() {
            assertEquals(
                "Код подтверждения для смены адреса электронной почты в TestSys",
                l.user.emailChangeConfirmationCodeSubject(),
            )
        }

        @Test
        fun `should greet without nickname and show the code on its own line in email change confirmation code body`() {
            assertEquals(
                "Здравствуйте!\n\n" +
                    "Вы запросили смену адреса электронной почты в TestSys на этот адрес. Чтобы подтвердить новый " +
                    "адрес, введите в TestSys этот код:\n\n" +
                    "01234567\n\n" +
                    "Код действует ограниченное время. Если код больше не действует, запросите смену адреса заново, " +
                    "и Вам придёт новый код.\n\n" +
                    "Если Вам пришло несколько таких писем, введите код из последнего.\n\n" +
                    "Если Вы не запрашивали смену адреса электронной почты в TestSys, ничего делать не нужно. " +
                    "Просто удалите это письмо.",
                l.user.emailChangeConfirmationCodeBody(confirmationCode = "01234567"),
            )
        }

        @Test
        fun `should state the change in email change notice subject`() {
            assertEquals("Адрес электронной почты в TestSys изменён", l.user.emailChangeNoticeSubject())
        }

        @Test
        fun `should greet by nickname and render the access code term in accusative in email change notice body`() {
            assertEquals(
                "Здравствуйте, Маша!\n\n" +
                    "Ваш адрес электронной почты в TestSys изменён. Письма от TestSys теперь будут приходить " +
                    "на новый адрес, а не на этот.\n\n" +
                    "Изменить адрес может только тот, кто знает Ваш Код-доступа.",
                l.user.emailChangeNoticeBody(name = "Маша"),
            )
        }
    }

    @Nested
    inner class UiBrandTests {

        @Test
        fun `should name the product in name`() {
            assertEquals("TestSys", l.uiBrand.name())
        }
    }

    @Nested
    inner class UiGuideTests {

        @Test
        fun `should name the page guide button in page`() {
            assertEquals("Открыть гайд страницы", l.uiGuide.page())
        }

        @Test
        fun `should name the block explanation button in block`() {
            assertEquals("Показать пояснение блока", l.uiGuide.block())
        }

        @Test
        fun `should name the previous step button in previous`() {
            assertEquals("Предыдущий шаг", l.uiGuide.previous())
        }

        @Test
        fun `should name the next step button in next`() {
            assertEquals("Следующий шаг", l.uiGuide.next())
        }

        @Test
        fun `should name the last step button in done`() {
            assertEquals("Завершить гайд", l.uiGuide.done())
        }

        @Test
        fun `should name the close button in close`() {
            assertEquals("Закрыть гайд", l.uiGuide.close())
        }
    }

    @Nested
    inner class UiFooterTests {

        @Test
        fun `should show the year without digit grouping in year`() {
            assertEquals("2026", l.uiFooter.year(year = 2026))
        }

        @Test
        fun `should name the footer navigation in links`() {
            assertEquals("Ссылки футера", l.uiFooter.links())
        }
    }

    @Nested
    inner class UiTableFiltersTests {

        @Test
        fun `should label the filter panel in title`() {
            assertEquals("Настроить фильтры", l.uiTableFilters.title())
        }

        @Test
        fun `should label the apply button in apply`() {
            assertEquals("Применить", l.uiTableFilters.apply())
        }

        @Test
        fun `should label the reset button in reset`() {
            assertEquals("Сбросить", l.uiTableFilters.reset())
        }
    }

    @Nested
    inner class UiCalendarTests {

        @Test
        fun `should label the today button in today`() {
            assertEquals("Сегодня", l.uiCalendar.today())
        }

        @Test
        fun `should label the cancel button in cancel`() {
            assertEquals("Отменить", l.uiCalendar.cancel())
        }

        @Test
        fun `should name the previous month button in previous month`() {
            assertEquals("Показать предыдущий месяц", l.uiCalendar.previousMonth())
        }

        @Test
        fun `should name the next month button in next month`() {
            assertEquals("Показать следующий месяц", l.uiCalendar.nextMonth())
        }

        @Test
        fun `should name the field after the colon in open`() {
            assertEquals("Открыть календарь: Начало тура", l.uiCalendar.open(label = "Начало тура"))
        }
    }

    @Nested
    inner class UiFieldTests {

        @Test
        fun `should ask to check the format in bad input`() {
            assertEquals("Проверьте формат значения", l.uiField.badInput())
        }

        @Test
        fun `should report a too small value in below min`() {
            assertEquals("Значение меньше допустимого", l.uiField.belowMin())
        }

        @Test
        fun `should report a too large value in above max`() {
            assertEquals("Значение больше допустимого", l.uiField.aboveMax())
        }

        @Test
        fun `should report a value off the step in step mismatch`() {
            assertEquals("Значение не соответствует шагу", l.uiField.stepMismatch())
        }
    }

    @Nested
    inner class UiDateFieldTests {

        @Test
        fun `should name the date part in date`() {
            assertEquals("дата", l.uiDateField.date())
        }

        @Test
        fun `should name the time part in time`() {
            assertEquals("время", l.uiDateField.time())
        }

        @Test
        fun `should name the start picker after the field in range from`() {
            assertEquals("Период: с", l.uiDateField.rangeFrom(label = "Период"))
        }

        @Test
        fun `should name the end picker after the field in range to`() {
            assertEquals("Период: до", l.uiDateField.rangeTo(label = "Период"))
        }

        @Test
        fun `should show the start prefix in range from prefix`() {
            assertEquals("С", l.uiDateField.rangeFromPrefix())
        }

        @Test
        fun `should show the end prefix in range to prefix`() {
            assertEquals("До", l.uiDateField.rangeToPrefix())
        }

        @Test
        fun `should ask for at least one bound in range required`() {
            assertEquals("Укажите хотя бы одну границу периода", l.uiDateField.rangeRequired())
        }

        @Test
        fun `should report a reversed range in range reversed`() {
            assertEquals("Дата окончания раньше даты начала", l.uiDateField.rangeReversed())
        }
    }

    @Nested
    inner class UiEditingTests {

        @Test
        fun `should label the edit action in start`() {
            assertEquals("Изменить", l.uiEditing.start())
        }

        @Test
        fun `should label the save action in save`() {
            assertEquals("Сохранить", l.uiEditing.save())
        }

        @Test
        fun `should label the cancel action in cancel`() {
            assertEquals("Отменить", l.uiEditing.cancel())
        }
    }

    @Nested
    inner class UiTableTests {

        @Test
        fun `should report no rows in empty`() {
            assertEquals("Нет данных", l.uiTable.empty())
        }

        @Test
        fun `should join the bounds with an en dash in range`() {
            assertEquals("1–20 из 1${NBSP}412", l.uiTable.range(from = 1, to = 20, total = 1412))
        }

        @Test
        fun `should group digits of every number in range`() {
            assertEquals(
                "1${NBSP}001–1${NBSP}020 из 12${NBSP}345",
                l.uiTable.range(from = 1001, to = 1020, total = 12_345),
            )
        }

        @Test
        fun `should name the header checkbox in select all`() {
            assertEquals("Выбрать все строки страницы", l.uiTable.selectAll())
        }

        @Test
        fun `should name the row checkbox in select row`() {
            assertEquals("Выбрать строку", l.uiTable.selectRow())
        }
    }

    @Nested
    inner class UiPaginationTests {

        @Test
        fun `should name the previous page button in previous`() {
            assertEquals("Перейти на предыдущую страницу", l.uiPagination.previous())
        }

        @Test
        fun `should name the next page button in next`() {
            assertEquals("Перейти на следующую страницу", l.uiPagination.next())
        }

        @Test
        fun `should name the page button by number in page`() {
            assertEquals("Страница 7", l.uiPagination.page(page = 7))
        }

        @Test
        fun `should group digits of a large number in page`() {
            assertEquals("Страница 1${NBSP}000", l.uiPagination.page(page = 1000))
        }

        @Test
        fun `should separate the page and the count with a spaced slash in counter`() {
            assertEquals("3 / 71", l.uiPagination.counter(page = 3, count = 71))
        }

        @Test
        fun `should group digits of a large count in counter`() {
            assertEquals("1 / 1${NBSP}412", l.uiPagination.counter(page = 1, count = 1412))
        }
    }

    @Nested
    inner class UiLoadTests {

        @Test
        fun `should report the failure in failed`() {
            assertEquals("Не удалось загрузить", l.uiLoad.failed())
        }

        @Test
        fun `should suggest another attempt in failed hint`() {
            assertEquals("Попробуйте ещё раз", l.uiLoad.failedHint())
        }

        @Test
        fun `should label the retry action in retry`() {
            assertEquals("Повторить", l.uiLoad.retry())
        }

        @Test
        fun `should name the skeleton in loading`() {
            assertEquals("Загрузка", l.uiLoad.loading())
        }
    }

    @Nested
    inner class UiDialogTests {

        @Test
        fun `should label the cancel action in cancel`() {
            assertEquals("Отменить", l.uiDialog.cancel())
        }

        @Test
        fun `should label the close button in close`() {
            assertEquals("Закрыть", l.uiDialog.close())
        }

        @Test
        fun `should quote the name in guillemets in type to confirm`() {
            assertEquals(
                "Введите «Весенний тур», чтобы подтвердить",
                l.uiDialog.typeToConfirm(name = "Весенний тур"),
            )
        }
    }

    @Nested
    inner class UiLookupTests {

        @Test
        fun `should show the search placeholder in search`() {
            assertEquals("Поиск", l.uiLookup.search())
        }

        @Test
        fun `should label the open action in open`() {
            assertEquals("Выбрать", l.uiLookup.open())
        }

        @Test
        fun `should label the clear action in clear`() {
            assertEquals("Очистить", l.uiLookup.clear())
        }

        @Test
        fun `should report no matches in empty`() {
            assertEquals("Ничего не найдено", l.uiLookup.empty())
        }

        @Test
        fun `should quote the removed value in guillemets in remove`() {
            assertEquals("Убрать «Маша»", l.uiLookup.remove(value = "Маша"))
        }

        @Test
        fun `should label the reset action in reset`() {
            assertEquals("Сбросить", l.uiLookup.reset())
        }

        @Test
        fun `should label the apply action in apply`() {
            assertEquals("Применить", l.uiLookup.apply())
        }

        @Test
        fun `should group digits of the count in selected count`() {
            assertEquals("Выбрано: 1${NBSP}500", l.uiLookup.selectedCount(count = 1500))
        }

        @Test
        fun `should label the select all checkbox in select all`() {
            assertEquals("Выбрать все", l.uiLookup.selectAll())
        }

        @Test
        fun `should group digits of the hidden count in more`() {
            assertEquals("Ещё 1${NBSP}200", l.uiLookup.more(count = 1200))
        }

        @Test
        fun `should prefix the hidden count with a plus in more chip`() {
            assertEquals("+2", l.uiLookup.moreChip(count = 2))
        }

        @Test
        fun `should group digits of a large hidden count in more chip`() {
            assertEquals("+1${NBSP}200", l.uiLookup.moreChip(count = 1200))
        }
    }

    @Nested
    inner class UiNavigationTests {

        @Test
        fun `should name the breadcrumbs in breadcrumbs`() {
            assertEquals("Навигационная цепочка", l.uiNavigation.breadcrumbs())
        }

        @Test
        fun `should name the sections navigation in sections`() {
            assertEquals("Разделы", l.uiNavigation.sections())
        }
    }

    @Nested
    inner class UiMenuTests {

        @Test
        fun `should name the action menu button in actions`() {
            assertEquals("Действия", l.uiMenu.actions())
        }
    }

    @Nested
    inner class UiTransferTests {

        @Test
        fun `should label the file picker in upload`() {
            assertEquals("Выбрать файлы", l.uiTransfer.upload())
        }

        @Test
        fun `should invite to drop files in drop`() {
            assertEquals("Перетащите файлы сюда", l.uiTransfer.drop())
        }

        @Test
        fun `should label the cancel action in cancel`() {
            assertEquals("Отменить", l.uiTransfer.cancel())
        }

        @Test
        fun `should show the cancelled status in cancelled`() {
            assertEquals("Отменено", l.uiTransfer.cancelled())
        }

        @Test
        fun `should label the remove action in remove file`() {
            assertEquals("Убрать файл", l.uiTransfer.removeFile())
        }

        @Test
        fun `should show the stalled status in stalled`() {
            assertEquals("Передача приостановлена", l.uiTransfer.stalled())
        }

        @Test
        fun `should show the preparing status in preparing`() {
            assertEquals("Подготовка", l.uiTransfer.preparing())
        }

        @Test
        fun `should show the transfer status in downloading`() {
            assertEquals("Передача", l.uiTransfer.downloading())
        }

        @Test
        fun `should show the done status in done`() {
            assertEquals("Передано сервером", l.uiTransfer.done())
        }

        @Test
        fun `should show the failed status in failed`() {
            assertEquals("Ошибка передачи", l.uiTransfer.failed())
        }

        @Test
        fun `should label the retry action in retry`() {
            assertEquals("Повторить", l.uiTransfer.retry())
        }

        @Test
        fun `should label the repeated download in download again`() {
            assertEquals("Скачать снова", l.uiTransfer.downloadAgain())
        }

        @Test
        fun `should report a rejected file in upload rejected`() {
            assertEquals("Файл не соответствует ограничениям", l.uiTransfer.uploadRejected())
        }

        @ParameterizedTest
        @CsvSource(
            "1,   10.0,  MEGABYTE, 'До 1 файла, до 10 МБ каждый'",
            "21,  512.0, BYTE,     'До 21 файла, до 512 Б каждый'",
            "1,   2.5,   KILOBYTE, 'До 1 файла, до 2,5 КБ каждый'",
            "1,   4.0,   GIGABYTE, 'До 1 файла, до 4 ГБ каждый'",
            "2,   100.0, BYTE,     'До 2 файлов, до 100 Б каждый'",
            "4,   64.0,  KILOBYTE, 'До 4 файлов, до 64 КБ каждый'",
            "3,   20.0,  MEGABYTE, 'До 3 файлов, до 20 МБ каждый'",
            "3,   1.5,   GIGABYTE, 'До 3 файлов, до 1,5 ГБ каждый'",
            "5,   512.0, BYTE,     'До 5 файлов, до 512 Б каждый'",
            "11,  1.5,   KILOBYTE, 'До 11 файлов, до 1,5 КБ каждый'",
            "25,  300.0, MEGABYTE, 'До 25 файлов, до 300 МБ каждый'",
            "100, 2.0,   GIGABYTE, 'До 100 файлов, до 2 ГБ каждый'",
        )
        fun `should agree the noun with the count and show the unit in upload limits`(
            count: Int,
            size: Double,
            unit: UploadLimitsUnit,
            expected: String,
        ) {
            assertEquals(expected, l.uiTransfer.uploadLimits(count = count, size = size, unit = unit))
        }

        @ParameterizedTest
        @CsvSource(
            "1, 'До 1 файла, до 10 Б каждый'",
            "3, 'До 3 файлов, до 10 Б каждый'",
            "5, 'До 5 файлов, до 10 Б каждый'",
        )
        fun `should fall back to bytes when unit is other in upload limits`(count: Int, expected: String) {
            assertEquals(expected, l.uiTransfer.uploadLimits(count = count, size = 10, unit = UploadLimitsUnit.OTHER))
        }

        @Test
        fun `should round size to one fraction digit half to even in upload limits`() {
            assertEquals(
                "До 2 файлов, до 2,8 МБ каждый",
                l.uiTransfer.uploadLimits(count = 2, size = 2.75, unit = UploadLimitsUnit.MEGABYTE),
            )
        }

        @Test
        fun `should group digits of count and size in upload limits`() {
            assertEquals(
                "До 1${NBSP}412 файлов, до 1${NBSP}000,2 КБ каждый",
                l.uiTransfer.uploadLimits(count = 1412, size = 1000.25, unit = UploadLimitsUnit.KILOBYTE),
            )
        }

        @Test
        fun `should join the label and the state with a colon in download label`() {
            assertEquals("Протокол: Передача", l.uiTransfer.downloadLabel(label = "Протокол", state = "Передача"))
        }

        @Test
        fun `should put the percent sign without a space in percent`() {
            assertEquals("45%", l.uiTransfer.percent(value = 45))
        }

        @Test
        fun `should group digits of a byte count beyond int in transferred bytes`() {
            assertEquals(
                "Передано: 3${NBSP}000${NBSP}000${NBSP}000 Б",
                l.uiTransfer.transferredBytes(count = 3_000_000_000L),
            )
        }

        @Test
        fun `should show the byte label in unit byte`() {
            assertEquals("Б", l.uiTransfer.unitByte())
        }

        @Test
        fun `should show the kilobyte label in unit kilobyte`() {
            assertEquals("КБ", l.uiTransfer.unitKilobyte())
        }

        @Test
        fun `should show the megabyte label in unit megabyte`() {
            assertEquals("МБ", l.uiTransfer.unitMegabyte())
        }

        @Test
        fun `should show the gigabyte label in unit gigabyte`() {
            assertEquals("ГБ", l.uiTransfer.unitGigabyte())
        }
    }

    @Nested
    inner class UiTimerTests {

        @Test
        fun `should label the days tile in days`() {
            assertEquals("дни", l.uiTimer.days())
        }

        @Test
        fun `should label the hours tile in hours`() {
            assertEquals("часы", l.uiTimer.hours())
        }

        @Test
        fun `should label the minutes tile in minutes`() {
            assertEquals("минуты", l.uiTimer.minutes())
        }

        @Test
        fun `should label the seconds tile in seconds`() {
            assertEquals("секунды", l.uiTimer.seconds())
        }
    }

    @Nested
    inner class UiHeaderTests {

        @Test
        fun `should label the sign in link in sign in`() {
            assertEquals("Войти", l.uiHeader.signIn())
        }

        @Test
        fun `should name the section search in search`() {
            assertEquals("Поиск по разделам", l.uiHeader.search())
        }

        @Test
        fun `should report no matches in search empty`() {
            assertEquals("Ничего не найдено", l.uiHeader.searchEmpty())
        }

        @Test
        fun `should head the popup in notifications`() {
            assertEquals("Уведомления", l.uiHeader.notifications())
        }

        @Test
        fun `should report no notifications in notifications empty`() {
            assertEquals("Нет уведомлений", l.uiHeader.notificationsEmpty())
        }

        @Test
        fun `should label the read all action in read all`() {
            assertEquals("Прочитать все", l.uiHeader.readAll())
        }

        @Test
        fun `should group digits of the count in unread count`() {
            assertEquals("Уведомления: непрочитанных 1${NBSP}234", l.uiHeader.unreadCount(count = 1234))
        }

        @Test
        fun `should mark the notification as unread in unread item`() {
            assertEquals("Новое решение — не прочитано", l.uiHeader.unreadItem(label = "Новое решение"))
        }

        @Test
        fun `should name the user in user menu`() {
            assertEquals("Меню пользователя: Маша", l.uiHeader.userMenu(name = "Маша"))
        }

        @Test
        fun `should label the open action in arrival open`() {
            assertEquals("Открыть", l.uiHeader.arrivalOpen())
        }

        @Test
        fun `should label the close action in arrival close`() {
            assertEquals("Закрыть уведомление", l.uiHeader.arrivalClose())
        }

        @ParameterizedTest
        @CsvSource(
            "1,  Получено 1 новое уведомление",
            "21, Получено 21 новое уведомление",
            "2,  Получено 2 новых уведомления",
            "4,  Получено 4 новых уведомления",
            "5,  Получено 5 новых уведомлений",
            "11, Получено 11 новых уведомлений",
        )
        fun `should agree the phrase with the count in arrival count`(count: Int, expected: String) {
            assertEquals(expected, l.uiHeader.arrivalCount(count = count))
        }

        @Test
        fun `should group digits of a large count in arrival count`() {
            assertEquals("Получено 1${NBSP}001 новое уведомление", l.uiHeader.arrivalCount(count = 1001))
        }

        @Test
        fun `should point to the notification list in arrival batch hint`() {
            assertEquals(
                "Откройте список уведомлений, чтобы посмотреть новые сообщения.",
                l.uiHeader.arrivalBatchHint(),
            )
        }
    }

    @Nested
    inner class UiErrorTests {

        @Test
        fun `should label the back action in back`() {
            assertEquals("Вернуться", l.uiError.back())
        }

        @Test
        fun `should state the missing page in not found title`() {
            assertEquals("Страница не найдена", l.uiError.notFoundTitle())
        }

        @Test
        fun `should suggest checking the address in not found description`() {
            assertEquals("Проверьте адрес или вернитесь на предыдущую страницу.", l.uiError.notFoundDescription())
        }

        @Test
        fun `should append the product name in not found page title`() {
            assertEquals("Страница не найдена — TestSys", l.uiError.notFoundPageTitle())
        }

        @Test
        fun `should state the denied access in forbidden title`() {
            assertEquals("Нет доступа", l.uiError.forbiddenTitle())
        }

        @Test
        fun `should explain the denied access in forbidden description`() {
            assertEquals(
                "У вас нет доступа к этой странице или нет нужной для неё роли.",
                l.uiError.forbiddenDescription(),
            )
        }

        @Test
        fun `should append the product name in forbidden page title`() {
            assertEquals("Нет доступа — TestSys", l.uiError.forbiddenPageTitle())
        }

        @Test
        fun `should state the failure in page failed title`() {
            assertEquals("Не удалось открыть страницу", l.uiError.pageFailedTitle())
        }

        @Test
        fun `should suggest reloading in page failed description`() {
            assertEquals("Обновите страницу или вернитесь на предыдущую.", l.uiError.pageFailedDescription())
        }

        @Test
        fun `should append the product name in page failed page title`() {
            assertEquals("Не удалось открыть страницу — TestSys", l.uiError.pageFailedPageTitle())
        }

        @Test
        fun `should report a missing object in failure not found`() {
            assertEquals("Объект не найден", l.uiError.failureNotFound())
        }

        @Test
        fun `should report the denied access in failure access denied`() {
            assertEquals("Нет доступа", l.uiError.failureAccessDenied())
        }

        @Test
        fun `should report the missing role in failure missing role`() {
            assertEquals("Нет нужной роли", l.uiError.failureMissingRole())
        }

        @Test
        fun `should report a resource outside the task in failure resource not in task`() {
            assertEquals("Ресурс недоступен в этой задаче", l.uiError.failureResourceNotInTask())
        }

        @Test
        fun `should report any other failure in failure failed`() {
            assertEquals("Действие не выполнено", l.uiError.failureFailed())
        }
    }

    @Nested
    inner class PageGuestTests {

        @Test
        fun `should name the sign in tab in tab sign in`() {
            assertEquals("Вход", l.pageGuest.tabSignIn())
        }

        @Test
        fun `should name the registration tab in tab registration`() {
            assertEquals("Регистрация", l.pageGuest.tabRegistration())
        }

        @Test
        fun `should name the restore access tab in tab restore access`() {
            assertEquals("Восстановление доступа", l.pageGuest.tabRestoreAccess())
        }

        @Test
        fun `should render the access code term in dative in start guide`() {
            assertEquals(
                "Начните работу в TestSys: войдите по Коду-доступа или зарегистрируйтесь.",
                l.pageGuest.startGuide(),
            )
        }

        @Test
        fun `should render the access code term in dative in start block guide`() {
            assertEquals(
                "Выберите вход по Коду-доступа или регистрацию нового Пользователя.",
                l.pageGuest.startBlockGuide(),
            )
        }

        @Test
        fun `should head the start block guide in start block guide title`() {
            assertEquals("Начало работы", l.pageGuest.startBlockGuideTitle())
        }

        @Test
        fun `should render the access code term in dative in start text`() {
            assertEquals(
                "Войдите по Коду-доступа или зарегистрируйтесь как Ученик или Организатор.",
                l.pageGuest.startText(),
            )
        }

        @Test
        fun `should label the registration button in register`() {
            assertEquals("Зарегистрироваться", l.pageGuest.register())
        }

        @Test
        fun `should label the sign in button in sign in button`() {
            assertEquals("Войти", l.pageGuest.signInButton())
        }

        @Test
        fun `should render the access code term in dative in sign in guide`() {
            assertEquals("Войдите в TestSys по своему Коду-доступа.", l.pageGuest.signInGuide())
        }

        @Test
        fun `should render the access code term in nominative in sign in access code`() {
            assertEquals("Код-доступа", l.pageGuest.signInAccessCode())
        }

        @Test
        fun `should explain where the code comes from in sign in access code guide`() {
            assertEquals(
                "Используйте код, выданный при регистрации или Организатором.",
                l.pageGuest.signInAccessCodeGuide(),
            )
        }

        @Test
        fun `should render the access code term in accusative in sign in block guide`() {
            assertEquals("Введите свой Код-доступа и нажмите «Войти».", l.pageGuest.signInBlockGuide())
        }

        @Test
        fun `should render the access code term in nominative in sign in invalid`() {
            assertEquals("Код-доступа недействителен", l.pageGuest.signInInvalid())
        }

        @Test
        fun `should render the access code term in accusative in restore access guide`() {
            assertEquals(
                "Восстановление доступа ещё не реализовано. Для входа используйте свой Код-доступа.",
                l.pageGuest.restoreAccessGuide(),
            )
        }

        @Test
        fun `should render the access code term in nominative in restore access block guide`() {
            assertEquals(
                "Восстановление доступа пока недоступно. Используйте вкладку входа, если у вас есть Код-доступа.",
                l.pageGuest.restoreAccessBlockGuide(),
            )
        }

        @Test
        fun `should state the missing feature in restore access empty`() {
            assertEquals("Восстановление доступа пока не реализовано", l.pageGuest.restoreAccessEmpty())
        }
    }

    @Nested
    inner class PageRegistrationTests {

        @Test
        fun `should describe the registration in guide`() {
            assertEquals(
                "Регистрация Ученика или Организатора: подтвердите почту и укажите сведения о себе.",
                l.pageRegistration.guide(),
            )
        }

        @Test
        fun `should label the email field in email`() {
            assertEquals("Почта", l.pageRegistration.email())
        }

        @Test
        fun `should explain the email field in email guide`() {
            assertEquals("Введите адрес почты, к которому у вас есть доступ.", l.pageRegistration.emailGuide())
        }

        @Test
        fun `should explain the first step in email step block guide`() {
            assertEquals(
                "Укажите почту, на которую придёт код подтверждения.",
                l.pageRegistration.emailStepBlockGuide(),
            )
        }

        @Test
        fun `should head the first step guide in email step block guide title`() {
            assertEquals("Подтверждение почты", l.pageRegistration.emailStepBlockGuideTitle())
        }

        @Test
        fun `should label the nickname field in nickname`() {
            assertEquals("Псевдоним", l.pageRegistration.nickname())
        }

        @Test
        fun `should explain the nickname field in nickname guide`() {
            assertEquals("Это имя будет видно другим Пользователям.", l.pageRegistration.nicknameGuide())
        }

        @Test
        fun `should label the role field in role`() {
            assertEquals("Роль", l.pageRegistration.role())
        }

        @Test
        fun `should explain the role choice in role guide`() {
            assertEquals(
                "Выберите Ученика для обучения или Организатора для проведения Туров.",
                l.pageRegistration.roleGuide(),
            )
        }

        @Test
        fun `should point to the last letter in code guide`() {
            assertEquals("Введите код из последнего письма с подтверждением.", l.pageRegistration.codeGuide())
        }

        @Test
        fun `should explain the second step in details step block guide`() {
            assertEquals(
                "Введите код из письма, выберите Псевдоним и Роль.",
                l.pageRegistration.detailsStepBlockGuide(),
            )
        }

        @Test
        fun `should head the second step guide in details step block guide title`() {
            assertEquals("Создание Пользователя", l.pageRegistration.detailsStepBlockGuideTitle())
        }

        @Test
        fun `should report the finished registration in completed`() {
            assertEquals("Регистрация завершена", l.pageRegistration.completed())
        }

        @Test
        fun `should report a wrong nickname in nickname invalid title`() {
            assertEquals("Псевдоним указан неверно", l.pageRegistration.nicknameInvalidTitle())
        }

        @Test
        fun `should state the nickname limits in nickname invalid description`() {
            assertEquals(
                "Псевдоним не может быть пустым или длиннее 512 символов.",
                l.pageRegistration.nicknameInvalidDescription(),
            )
        }

        @Test
        fun `should report a missing request in request not found`() {
            assertEquals("Запрос регистрации не найден", l.pageRegistration.requestNotFound())
        }

        @Test
        fun `should report a bound email in email bound title`() {
            assertEquals("Почта уже привязана к Пользователю", l.pageRegistration.emailBoundTitle())
        }

        @Test
        fun `should render the access code term in dative in email bound description`() {
            assertEquals("Войдите по Коду-доступа.", l.pageRegistration.emailBoundDescription())
        }
    }

    @Nested
    inner class PageProfileTests {

        @Test
        fun `should name the page in title`() {
            assertEquals("Профиль", l.pageProfile.title())
        }

        @Test
        fun `should describe the page in guide`() {
            assertEquals(
                "Здесь можно просмотреть свой Профиль и изменить привязанную почту.",
                l.pageProfile.guide(),
            )
        }

        @Test
        fun `should describe the profile block in profile block guide`() {
            assertEquals("Ваш Псевдоним и текущая привязанная почта.", l.pageProfile.profileBlockGuide())
        }

        @Test
        fun `should label the nickname in nickname`() {
            assertEquals("Псевдоним", l.pageProfile.nickname())
        }

        @Test
        fun `should label the email in email`() {
            assertEquals("Почта", l.pageProfile.email())
        }

        @Test
        fun `should head the change block in change title`() {
            assertEquals("Смена почты", l.pageProfile.changeTitle())
        }

        @Test
        fun `should describe the change steps in change block guide`() {
            assertEquals(
                "Укажите новую почту, получите код и подтвердите изменение кодом из письма.",
                l.pageProfile.changeBlockGuide(),
            )
        }

        @Test
        fun `should label the new email field in change new email`() {
            assertEquals("Новая почта", l.pageProfile.changeNewEmail())
        }

        @Test
        fun `should label the change button in change button`() {
            assertEquals("Сменить почту", l.pageProfile.changeButton())
        }

        @Test
        fun `should report the change in changed title`() {
            assertEquals("Почта изменена", l.pageProfile.changedTitle())
        }

        @Test
        fun `should show the new email after a dash in changed description`() {
            assertEquals("Новая почта — masha@example.com.", l.pageProfile.changedDescription(email = "masha@example.com"))
        }

        @Test
        fun `should report the current email in same email`() {
            assertEquals("Это ваша текущая почта", l.pageProfile.sameEmail())
        }

        @Test
        fun `should report an email bound to another user in email bound`() {
            assertEquals("Почта уже привязана к другому Пользователю", l.pageProfile.emailBound())
        }

        @Test
        fun `should ask for another email in another email`() {
            assertEquals("Укажите другую почту.", l.pageProfile.anotherEmail())
        }

        @Test
        fun `should report a missing request in request not found`() {
            assertEquals("Запрос смены почты не найден", l.pageProfile.requestNotFound())
        }
    }

    @Nested
    inner class PageConfirmationTests {

        @Test
        fun `should label the code field in code`() {
            assertEquals("Код из письма", l.pageConfirmation.code())
        }

        @Test
        fun `should label the send button in send code`() {
            assertEquals("Получить код", l.pageConfirmation.sendCode())
        }

        @Test
        fun `should label the resend button in resend code`() {
            assertEquals("Отправить код повторно", l.pageConfirmation.resendCode())
        }

        @Test
        fun `should label the change email button in change email`() {
            assertEquals("Изменить почту", l.pageConfirmation.changeEmail())
        }

        @Test
        fun `should report the sent code in code sent`() {
            assertEquals("Код отправлен", l.pageConfirmation.codeSent())
        }

        @Test
        fun `should report the resent code in code resent title`() {
            assertEquals("Код отправлен повторно", l.pageConfirmation.codeResentTitle())
        }

        @Test
        fun `should name the address in code resent description`() {
            assertEquals(
                "Письмо с кодом отправлено на masha@example.com.",
                l.pageConfirmation.codeResentDescription(email = "masha@example.com"),
            )
        }

        @Test
        fun `should report a wrong email in email invalid title`() {
            assertEquals("Почта указана неверно", l.pageConfirmation.emailInvalidTitle())
        }

        @Test
        fun `should state the email format in email invalid description`() {
            assertEquals(
                "Нужна почта до 255 символов с одним символом @ между непустыми частями.",
                l.pageConfirmation.emailInvalidDescription(),
            )
        }

        @Test
        fun `should report the expired code in code expired`() {
            assertEquals("Срок действия кода истёк", l.pageConfirmation.codeExpired())
        }

        @Test
        fun `should report the exhausted attempts in attempts exhausted`() {
            assertEquals("Попытки ввода кода исчерпаны", l.pageConfirmation.attemptsExhausted())
        }

        @Test
        fun `should ask to resend the code in resend hint`() {
            assertEquals("Отправьте код повторно.", l.pageConfirmation.resendHint())
        }

        @Test
        fun `should report a wrong code in code mismatch`() {
            assertEquals("Код не совпадает с отправленным", l.pageConfirmation.codeMismatch())
        }

        @Test
        fun `should ask to start over in request not found hint`() {
            assertEquals("Измените почту и получите код заново.", l.pageConfirmation.requestNotFoundHint())
        }
    }

    @Nested
    inner class PageHomeTests {

        @Test
        fun `should name the page in title`() {
            assertEquals("Главная", l.pageHome.title())
        }

        @Test
        fun `should render the invite code term in dative in guide`() {
            assertEquals(
                "Здесь показаны ваши Роли и Сообщества. По Коду-приглашению можно присоединиться к Сообществу.",
                l.pageHome.guide(),
            )
        }

        @Test
        fun `should render the invite code term in dative in join hint`() {
            assertEquals("Присоединитесь к сообществу по Коду-приглашению.", l.pageHome.joinHint())
        }

        @Test
        fun `should head the roles block in roles title`() {
            assertEquals("Роли", l.pageHome.rolesTitle())
        }

        @Test
        fun `should describe the roles block in roles guide`() {
            assertEquals(
                "Ваши Роли определяют доступные разделы и действия в Сообществах.",
                l.pageHome.rolesGuide(),
            )
        }

        @Test
        fun `should head the role column in roles role`() {
            assertEquals("Роль", l.pageHome.rolesRole())
        }

        @Test
        fun `should head the community count column in roles communities`() {
            assertEquals("Сообществ", l.pageHome.rolesCommunities())
        }

        @Test
        fun `should report no roles in roles empty`() {
            assertEquals("Ролей пока нет", l.pageHome.rolesEmpty())
        }

        @Test
        fun `should head the join block in join title`() {
            assertEquals("Присоединиться к сообществу", l.pageHome.joinTitle())
        }

        @Test
        fun `should render the invite code term in accusative in join guide`() {
            assertEquals(
                "Введите Код-приглашение, полученный от Администратора, и нажмите «Присоединиться».",
                l.pageHome.joinGuide(),
            )
        }

        @Test
        fun `should render the invite code term in nominative in join invite code`() {
            assertEquals("Код-приглашение", l.pageHome.joinInviteCode())
        }

        @Test
        fun `should label the join button in join button`() {
            assertEquals("Присоединиться", l.pageHome.joinButton())
        }

        @Test
        fun `should head the communities block in communities title`() {
            assertEquals("Сообщества", l.pageHome.communitiesTitle())
        }

        @Test
        fun `should describe the communities block in communities guide`() {
            assertEquals("Сообщества, доступные в этом Кабинете.", l.pageHome.communitiesGuide())
        }

        @Test
        fun `should head the id column in communities id`() {
            assertEquals("ID", l.pageHome.communitiesId())
        }

        @Test
        fun `should head the name column in communities name`() {
            assertEquals("Название", l.pageHome.communitiesName())
        }

        @Test
        fun `should head the description column in communities description`() {
            assertEquals("Описание", l.pageHome.communitiesDescription())
        }

        @Test
        fun `should head the roles column in communities roles`() {
            assertEquals("Роли", l.pageHome.communitiesRoles())
        }

        @Test
        fun `should report no communities in communities empty`() {
            assertEquals("Сообществ пока нет", l.pageHome.communitiesEmpty())
        }

        @Test
        fun `should render the access code term in nominative in access code title`() {
            assertEquals("Код-доступа", l.pageHome.accessCodeTitle())
        }

        @Test
        fun `should render the access code term in accusative in access code guide`() {
            assertEquals(
                "Сохраните выданный Код-доступа: он нужен для входа. При повторном открытии страницы этот блок исчезнет.",
                l.pageHome.accessCodeGuide(),
            )
        }

        @Test
        fun `should render the access code term in accusative in access code alert title`() {
            assertEquals("Сохраните Код-доступа", l.pageHome.accessCodeAlertTitle())
        }

        @Test
        fun `should explain the access code in access code alert text`() {
            assertEquals(
                "По нему вы входите в Систему. Код также отправлен на почту.",
                l.pageHome.accessCodeAlertText(),
            )
        }

        @Test
        fun `should render the invite code term in nominative in invite code invalid`() {
            assertEquals("Код-приглашение недействителен", l.pageHome.inviteCodeInvalid())
        }

        @Test
        fun `should render the invite code term in genitive in invite code expired`() {
            assertEquals("Срок Кода-приглашения истёк", l.pageHome.inviteCodeExpired())
        }

        @Test
        fun `should report the join in joined`() {
            assertEquals("Вы присоединились к сообществу", l.pageHome.joined())
        }
    }

    @Nested
    inner class PageCabinetTests {

        @Test
        fun `should name the home item in home`() {
            assertEquals("Главная", l.pageCabinet.home())
        }

        @Test
        fun `should name the menu item in menu`() {
            assertEquals("Меню", l.pageCabinet.menu())
        }

        @Test
        fun `should name the profile item in profile`() {
            assertEquals("Профиль", l.pageCabinet.profile())
        }

        @Test
        fun `should name the sign out item in sign out`() {
            assertEquals("Выйти", l.pageCabinet.signOut())
        }

        @ParameterizedTest
        @CsvSource(
            "ADMINISTRATOR, Администратор",
            "DEVELOPER,     Разработчик",
            "MANAGER,       Организатор",
            "JUDGE,         Судья",
            "STUDENT,       Ученик",
            "PARTICIPANT,   Участник",
            "OBSERVER,      Наблюдатель",
            "SUPERVISOR,    Супервайзер",
            "OTHER,         Роль",
        )
        fun `should name the role in nominative in role name`(role: RoleNameRole, expected: String) {
            assertEquals(expected, l.pageCabinet.roleName(role = role))
        }

        @Test
        fun `should name the contests section in section contests`() {
            assertEquals("Туры", l.pageCabinet.sectionContests())
        }

        @Test
        fun `should name the users section in section users`() {
            assertEquals("Пользователи", l.pageCabinet.sectionUsers())
        }

        @Test
        fun `should name the communities section in section communities`() {
            assertEquals("Сообщества", l.pageCabinet.sectionCommunities())
        }

        @Test
        fun `should name the tasks section in section tasks`() {
            assertEquals("Задачи", l.pageCabinet.sectionTasks())
        }

        @Test
        fun `should name the classes section in section classes`() {
            assertEquals("Классы", l.pageCabinet.sectionClasses())
        }

        @Test
        fun `should name the competitions section in section competitions`() {
            assertEquals("Соревнования", l.pageCabinet.sectionCompetitions())
        }

        @Test
        fun `should name the submissions section in section submissions`() {
            assertEquals("Посылки", l.pageCabinet.sectionSubmissions())
        }

        @Test
        fun `should point to the header in pending guide`() {
            assertEquals(
                "Этот раздел пока не реализован. Доступные разделы можно открыть через шапку.",
                l.pageCabinet.pendingGuide(),
            )
        }

        @Test
        fun `should state the missing actions in pending block guide`() {
            assertEquals("Действия этого раздела ещё не реализованы.", l.pageCabinet.pendingBlockGuide())
        }

        @Test
        fun `should head the pending block guide in pending block guide title`() {
            assertEquals("Состояние раздела", l.pageCabinet.pendingBlockGuideTitle())
        }

        @Test
        fun `should state the missing section in pending empty`() {
            assertEquals("Раздел пока не реализован", l.pageCabinet.pendingEmpty())
        }

        @Test
        fun `should name the observer cabinet in observer title`() {
            assertEquals("Кабинет Наблюдателя", l.pageCabinet.observerTitle())
        }

        @Test
        fun `should name the supervisor cabinet in supervisor title`() {
            assertEquals("Кабинет Супервайзера", l.pageCabinet.supervisorTitle())
        }
    }

    @Nested
    inner class PageJudgeTests {

        @Test
        fun `should name the judge cabinet in title`() {
            assertEquals("Кабинет Судьи", l.pageJudge.title())
        }

        @Test
        fun `should decline the submission and verdict terms in guide`() {
            assertEquals(
                "Найдите Посылку в таблице и откройте её для просмотра Решения и Вердикта.",
                l.pageJudge.guide(),
            )
        }

        @Test
        fun `should use the submission term in submission`() {
            assertEquals("Посылка", l.pageJudge.submission())
        }

        @Test
        fun `should name the author in author`() {
            assertEquals("Автор", l.pageJudge.author())
        }

        @Test
        fun `should name the author id in author id`() {
            assertEquals("ID автора", l.pageJudge.authorId())
        }

        @Test
        fun `should name the final score in final score`() {
            assertEquals("Итоговый балл", l.pageJudge.finalScore())
        }

        @Test
        fun `should use the plural submission term in submissions guide`() {
            assertEquals("Посылки и результаты их проверки.", l.pageJudge.submissionsGuide())
        }

        @Test
        fun `should name the sending time in submissions sent`() {
            assertEquals("Отправлено", l.pageJudge.submissionsSent())
        }

        @Test
        fun `should name the automatic score in submissions auto score`() {
            assertEquals("Автоматический балл", l.pageJudge.submissionsAutoScore())
        }

        @Test
        fun `should name the judge score in submissions judge score`() {
            assertEquals("Судейский балл", l.pageJudge.submissionsJudgeScore())
        }

        @Test
        fun `should use the genitive plural submission term in submissions empty`() {
            assertEquals("Посылок нет", l.pageJudge.submissionsEmpty())
        }

        @Test
        fun `should use the accusative plural submission term in submissions empty hint`() {
            assertEquals(
                "Здесь появятся успешно проверенные Посылки Учеников и Участников.",
                l.pageJudge.submissionsEmptyHint(),
            )
        }

        @Test
        fun `should use the genitive submission term in filter submission id`() {
            assertEquals("ID Посылки", l.pageJudge.filterSubmissionId())
        }

        @Test
        fun `should name the class id in filter class id`() {
            assertEquals("ID Класса", l.pageJudge.filterClassId())
        }

        @Test
        fun `should name the competition id in filter competition id`() {
            assertEquals("ID Соревнования", l.pageJudge.filterCompetitionId())
        }

        @Test
        fun `should ask for a positive integer in filter id invalid`() {
            assertEquals("Введите положительное целое число", l.pageJudge.filterIdInvalid())
        }

        @Test
        fun `should number the submission in solution title`() {
            assertEquals("Посылка №42", l.pageJudge.solutionTitle(id = 42L))
        }

        @Test
        fun `should keep the largest id exact and ungrouped in solution title`() {
            assertEquals("Посылка №9223372036854775807", l.pageJudge.solutionTitle(id = Long.MAX_VALUE))
        }

        @ParameterizedTest
        @CsvSource(
            "QUEUED,        В очереди",
            "IN_PROGRESS,   Проверяется",
            "SUCCESS,       Проверена",
            "GRADING_ERROR, Ошибка проверки",
            "TIMEOUT,       Превышено время проверки",
            "OTHER,         Неизвестный статус",
        )
        fun `should name the grading status in solution status`(status: SolutionStatusStatus, expected: String) {
            assertEquals(expected, l.pageJudge.solutionStatus(status = status))
        }

        @ParameterizedTest
        @CsvSource(
            "PYTHON,          Python",
            "JAVASCRIPT,      JavaScript",
            "VISUAL_LANGUAGE, Визуальный язык",
            "OTHER,           Неизвестный язык",
        )
        fun `should name the language in solution language`(language: SolutionLanguageLanguage, expected: String) {
            assertEquals(expected, l.pageJudge.solutionLanguage(language = language))
        }

        @Test
        fun `should decline the submission and judgment order terms in solution guide`() {
            assertEquals(
                "Просмотрите сведения о Посылке, результаты проверки и историю Судейских вердиктов.",
                l.pageJudge.solutionGuide(),
            )
        }

        @Test
        fun `should name the score in solution score`() {
            assertEquals("Балл", l.pageJudge.solutionScore())
        }

        @Test
        fun `should name the reason in solution reason`() {
            assertEquals("Обоснование", l.pageJudge.solutionReason())
        }

        @Test
        fun `should head the details block in solution details title`() {
            assertEquals("Сведения", l.pageJudge.solutionDetailsTitle())
        }

        @Test
        fun `should use the prepositional submission term in solution details guide`() {
            assertEquals(
                "Сведения об отправленной Посылке, её авторе и состоянии проверки.",
                l.pageJudge.solutionDetailsGuide(),
            )
        }

        @Test
        fun `should name the id in solution details id`() {
            assertEquals("ID", l.pageJudge.solutionDetailsId())
        }

        @Test
        fun `should name the sending time in solution details sent`() {
            assertEquals("Отправлена", l.pageJudge.solutionDetailsSent())
        }

        @Test
        fun `should name the task in solution details task`() {
            assertEquals("Задача", l.pageJudge.solutionDetailsTask())
        }

        @Test
        fun `should name the contest in solution details contest`() {
            assertEquals("Тур", l.pageJudge.solutionDetailsContest())
        }

        @Test
        fun `should name the file in solution details file`() {
            assertEquals("Файл", l.pageJudge.solutionDetailsFile())
        }

        @Test
        fun `should name the language in solution details language`() {
            assertEquals("Язык", l.pageJudge.solutionDetailsLanguage())
        }

        @Test
        fun `should name the grading status in solution details status`() {
            assertEquals("Статус проверки", l.pageJudge.solutionDetailsStatus())
        }

        @Test
        fun `should use the verdict term in solution verdict title`() {
            assertEquals("Вердикт", l.pageJudge.solutionVerdictTitle())
        }

        @Test
        fun `should use the dative plural polygon term in solution verdict guide`() {
            assertEquals(
                "Результаты автоматической проверки по Полигонам и итоговый балл.",
                l.pageJudge.solutionVerdictGuide(),
            )
        }

        @Test
        fun `should use the genitive verdict term in solution verdict empty`() {
            assertEquals("Вердикта нет", l.pageJudge.solutionVerdictEmpty())
        }

        @Test
        fun `should use the dative plural polygon term in solution verdict empty hint`() {
            assertEquals("Баллы по Полигонам появятся после успешной проверки.", l.pageJudge.solutionVerdictEmptyHint())
        }

        @Test
        fun `should use the polygon term in solution verdict polygon`() {
            assertEquals("Полигон", l.pageJudge.solutionVerdictPolygon())
        }

        @Test
        fun `should name the files in solution verdict files`() {
            assertEquals("Файлы", l.pageJudge.solutionVerdictFiles())
        }

        @Test
        fun `should quote the polygon name in solution verdict download logs`() {
            assertEquals("Скачать логи Полигона «Лабиринт»", l.pageJudge.solutionVerdictDownloadLogs(name = "Лабиринт"))
        }

        @Test
        fun `should quote the polygon name in solution verdict download recording`() {
            assertEquals(
                "Скачать видеозапись Полигона «Лабиринт»",
                l.pageJudge.solutionVerdictDownloadRecording(name = "Лабиринт"),
            )
        }

        @Test
        fun `should use the plural judgment order term in solution judgments title`() {
            assertEquals("Судейские вердикты", l.pageJudge.solutionJudgmentsTitle())
        }

        @Test
        fun `should keep the partial verdict reference lowercase in solution judgments guide`() {
            assertEquals("История выставленных Судьями вердиктов.", l.pageJudge.solutionJudgmentsGuide())
        }

        @Test
        fun `should name the issue time in solution judgments issued`() {
            assertEquals("Выставлен", l.pageJudge.solutionJudgmentsIssued())
        }

        @Test
        fun `should use the genitive plural judgment order term in solution judgments empty`() {
            assertEquals("Судейских вердиктов нет", l.pageJudge.solutionJudgmentsEmpty())
        }

        @Test
        fun `should keep the partial verdict reference lowercase in solution judgments issue`() {
            assertEquals("Выставить вердикт", l.pageJudge.solutionJudgmentsIssue())
        }

        @Test
        fun `should use the judgment order term in solution dialog title`() {
            assertEquals("Новый Судейский вердикт", l.pageJudge.solutionDialogTitle())
        }

        @Test
        fun `should ask for the score in solution dialog score required`() {
            assertEquals("Укажите балл", l.pageJudge.solutionDialogScoreRequired())
        }

        @Test
        fun `should ask for the reason in solution dialog reason required`() {
            assertEquals("Укажите обоснование", l.pageJudge.solutionDialogReasonRequired())
        }

        @Test
        fun `should name the issuing action in solution dialog submit`() {
            assertEquals("Выставить", l.pageJudge.solutionDialogSubmit())
        }

        @Test
        fun `should use the judgment order term in solution dialog saved`() {
            assertEquals("Судейский вердикт сохранён", l.pageJudge.solutionDialogSaved())
        }
    }

    @Nested
    inner class PageDeveloperTests {

        @Test
        fun `should name the developer cabinet in title`() {
            assertEquals("Кабинет Разработчика", l.pageDeveloper.title())
        }

        @Test
        fun `should name the id in id`() {
            assertEquals("ID", l.pageDeveloper.id())
        }

        @Test
        fun `should name the name in name`() {
            assertEquals("Название", l.pageDeveloper.name())
        }

        @Test
        fun `should explain the name filter in name hint`() {
            assertEquals("Часть названия, без учёта регистра", l.pageDeveloper.nameHint())
        }

        @Test
        fun `should ask for the name in name required`() {
            assertEquals("Укажите название", l.pageDeveloper.nameRequired())
        }

        @Test
        fun `should name the description in description`() {
            assertEquals("Описание", l.pageDeveloper.description())
        }

        @Test
        fun `should name one trik studio version in trik studio version`() {
            assertEquals("Версия TRIK Studio", l.pageDeveloper.trikStudioVersion())
        }

        @Test
        fun `should name the trik studio versions in trik studio versions`() {
            assertEquals("Версии TRIK Studio", l.pageDeveloper.trikStudioVersions())
        }

        @Test
        fun `should name the start in starts at`() {
            assertEquals("Начало", l.pageDeveloper.startsAt())
        }

        @Test
        fun `should name the end in ends at`() {
            assertEquals("Конец", l.pageDeveloper.endsAt())
        }

        @Test
        fun `should name the individual limit in attempt limit`() {
            assertEquals("Индивидуальный лимит", l.pageDeveloper.attemptLimit())
        }

        @Test
        fun `should name the passing time in attempt duration`() {
            assertEquals("Время на прохождение", l.pageDeveloper.attemptDuration())
        }

        @Test
        fun `should abbreviate minutes lowercase in minutes unit`() {
            assertEquals("мин", l.pageDeveloper.minutesUnit())
        }

        @Test
        fun `should name the state in state`() {
            assertEquals("Состояние", l.pageDeveloper.state())
        }

        @ParameterizedTest
        @CsvSource(
            "NEW,         Новая",
            "UNCOMMITTED, Не зафиксирована",
            "COMMITTED,   Зафиксирована",
            "OTHER,       Неизвестное состояние",
        )
        fun `should name the feminine task state in task state`(state: TaskStateState, expected: String) {
            assertEquals(expected, l.pageDeveloper.taskState(state = state))
        }

        @Test
        fun `should use the community term in community`() {
            assertEquals("Сообщество", l.pageDeveloper.community())
        }

        @Test
        fun `should use the plural community term in communities`() {
            assertEquals("Сообщества", l.pageDeveloper.communities())
        }

        @Test
        fun `should head the details block in details`() {
            assertEquals("Сведения", l.pageDeveloper.details())
        }

        @Test
        fun `should explain viewing and editing in editing guide`() {
            assertEquals(
                "Просмотрите параметры. Нажмите «Изменить», чтобы отредактировать их, затем сохраните изменения " +
                    "или отмените их.",
                l.pageDeveloper.editingGuide(),
            )
        }

        @Test
        fun `should name the access in access`() {
            assertEquals("Доступ", l.pageDeveloper.access())
        }

        @Test
        fun `should state that no access is shared in access empty`() {
            assertEquals("Доступ не предоставлен", l.pageDeveloper.accessEmpty())
        }

        @Test
        fun `should name the sharing action in share`() {
            assertEquals("Предоставить доступ", l.pageDeveloper.share())
        }

        @Test
        fun `should name the sharing submit action in share submit`() {
            assertEquals("Предоставить", l.pageDeveloper.shareSubmit())
        }

        @Test
        fun `should name the attaching action in attach`() {
            assertEquals("Прикрепить", l.pageDeveloper.attach())
        }

        @Test
        fun `should name the detaching action in detach`() {
            assertEquals("Открепить", l.pageDeveloper.detach())
        }

        @Test
        fun `should name the creating action in create`() {
            assertEquals("Создать", l.pageDeveloper.create())
        }

        @Test
        fun `should quote the task name after the task term in task title`() {
            assertEquals("Задача «Лабиринт»", l.pageDeveloper.taskTitle(name = "Лабиринт"))
        }

        @Test
        fun `should name the type in type`() {
            assertEquals("Тип", l.pageDeveloper.type())
        }

        @ParameterizedTest
        @CsvSource(
            "STATEMENT,          Условие",
            "EXERCISE,           Упражнение",
            "TEST,               Полигон",
            "DEVELOPER_SOLUTION, Авторское Решение",
            "OTHER,              Неизвестный тип",
        )
        fun `should use the resource type terms in resource type`(type: ResourceTypeType, expected: String) {
            assertEquals(expected, l.pageDeveloper.resourceType(type = type))
        }

        @Test
        fun `should name the language in language`() {
            assertEquals("Язык", l.pageDeveloper.language())
        }

        @Test
        fun `should ask for the language in language required`() {
            assertEquals("Выберите язык", l.pageDeveloper.languageRequired())
        }

        @Test
        fun `should name the expected score in expected score`() {
            assertEquals("Ожидаемый балл", l.pageDeveloper.expectedScore())
        }

        @Test
        fun `should ask for the score in score required`() {
            assertEquals("Укажите балл", l.pageDeveloper.scoreRequired())
        }

        @Test
        fun `should name the score in score`() {
            assertEquals("Балл", l.pageDeveloper.score())
        }

        @Test
        fun `should name the file in file`() {
            assertEquals("Файл", l.pageDeveloper.file())
        }

        @Test
        fun `should name the version id in version id`() {
            assertEquals("ID версии", l.pageDeveloper.versionId())
        }

        @Test
        fun `should name the download action in download`() {
            assertEquals("Скачать", l.pageDeveloper.download())
        }

        @Test
        fun `should name the attachment column in attached`() {
            assertEquals("Прикреплено", l.pageDeveloper.attached())
        }

        @Test
        fun `should name the committed revision in attached committed`() {
            assertEquals("Прикреплено в зафиксированной", l.pageDeveloper.attachedCommitted())
        }

        @Test
        fun `should name the working revision in attached working`() {
            assertEquals("Прикреплено в рабочей", l.pageDeveloper.attachedWorking())
        }

        @Test
        fun `should name the start of testing in started at`() {
            assertEquals("Запущено", l.pageDeveloper.startedAt())
        }

        @Test
        fun `should name the status in status`() {
            assertEquals("Статус", l.pageDeveloper.status())
        }

        @Test
        fun `should name the summary in summary`() {
            assertEquals("Итог", l.pageDeveloper.summary())
        }

        @Test
        fun `should use the genitive contest term in schedule start required`() {
            assertEquals("Укажите начало Тура", l.pageDeveloper.scheduleStartRequired())
        }

        @Test
        fun `should require the end after the start in schedule end before start`() {
            assertEquals("Конец должен быть позже начала", l.pageDeveloper.scheduleEndBeforeStart())
        }

        @Test
        fun `should use the plural diagnostic term in summary pending`() {
            assertEquals("Выполняются Диагностики", l.pageDeveloper.summaryPending())
        }

        @Test
        fun `should use the plural diagnostic and submission terms in summary awaiting`() {
            assertEquals("Диагностики пройдены, создаются Посылки", l.pageDeveloper.summaryAwaiting())
        }

        @Test
        fun `should use the prepositional plural polygon term in summary stopped`() {
            assertEquals("Диагностики нашли ошибки в Полигонах", l.pageDeveloper.summaryStopped())
        }

        @Test
        fun `should count the checked submissions in summary checking`() {
            assertEquals("Проверяются Посылки: 3", l.pageDeveloper.summaryChecking(count = 3))
        }

        @Test
        fun `should keep a large count ungrouped in summary checking`() {
            assertEquals("Проверяются Посылки: 2147483647", l.pageDeveloper.summaryChecking(count = Int.MAX_VALUE))
        }

        @Test
        fun `should use the plural submission term in summary passed`() {
            assertEquals("Все Посылки набрали ожидаемый балл", l.pageDeveloper.summaryPassed())
        }

        @Test
        fun `should show failed of total in summary failed`() {
            assertEquals("Провалено Посылок: 2 из 5", l.pageDeveloper.summaryFailed(failed = 2, total = 5))
        }

        @Test
        fun `should keep large counts ungrouped in summary failed`() {
            assertEquals("Провалено Посылок: 1412 из 2147483647", l.pageDeveloper.summaryFailed(failed = 1412, total = Int.MAX_VALUE))
        }

        @Test
        fun `should show the technical details verbatim in summary technical`() {
            assertEquals(
                "Техническая остановка: grader unavailable",
                l.pageDeveloper.summaryTechnical(description = "grader unavailable"),
            )
        }

        @Test
        fun `should use the genitive task term in refusal already committed`() {
            assertEquals("У Задачи нет незафиксированных изменений.", l.pageDeveloper.refusalAlreadyCommitted())
        }

        @Test
        fun `should use the accusative statement term in refusal no statement`() {
            assertEquals("Прикрепите Условие.", l.pageDeveloper.refusalNoStatement())
        }

        @Test
        fun `should use the accusative polygon term in refusal no polygons`() {
            assertEquals("Прикрепите хотя бы один Полигон.", l.pageDeveloper.refusalNoPolygons())
        }

        @Test
        fun `should use the accusative developer solution term in refusal no developer solutions`() {
            assertEquals("Прикрепите хотя бы одно Авторское Решение.", l.pageDeveloper.refusalNoDeveloperSolutions())
        }

        @Test
        fun `should ask for supported versions in refusal no trik studio versions`() {
            assertEquals("Укажите поддерживаемые версии TRIK Studio.", l.pageDeveloper.refusalNoTrikStudioVersions())
        }

        @ParameterizedTest
        @CsvSource(
            "PYTHON,          Прикрепите Упражнение для языка «Python».",
            "JAVASCRIPT,      Прикрепите Упражнение для языка «JavaScript».",
            "VISUAL_LANGUAGE, Прикрепите Упражнение для языка «Визуальный язык».",
            "OTHER,           Прикрепите Упражнение.",
        )
        fun `should name the language of the missing exercise in refusal no exercise`(
            language: RefusalNoExerciseLanguage,
            expected: String,
        ) {
            assertEquals(expected, l.pageDeveloper.refusalNoExercise(language = language))
        }

        @Test
        fun `should show the version verbatim in refusal version not supported`() {
            assertEquals(
                "Задача прикреплена к Туру с версией TRIK Studio 2024.1, которую она не поддерживает.",
                l.pageDeveloper.refusalVersionNotSupported(version = "2024.1"),
            )
        }

        @Test
        fun `should use the genitive task term in refusal not tested`() {
            assertEquals("Рабочая версия Задачи не прошла тестирование.", l.pageDeveloper.refusalNotTested())
        }

        @ParameterizedTest
        @CsvSource(
            "INFO,    Сведения · нет атрибута «id»",
            "WARNING, Предупреждение · нет атрибута «id»",
            "ERROR,   Ошибка · нет атрибута «id»",
            "OTHER,   Замечание · нет атрибута «id»",
        )
        fun `should prefix the finding with the severity in diagnostic line`(severity: DiagnosticLineSeverity, expected: String) {
            assertEquals(expected, l.pageDeveloper.diagnosticLine(severity = severity, finding = "нет атрибута «id»"))
        }

        @ParameterizedTest
        @CsvSource(
            "INFO,    Сведения · world[0]/robots[0]@id · нет атрибута «id»",
            "WARNING, Предупреждение · world[0]/robots[0]@id · нет атрибута «id»",
            "ERROR,   Ошибка · world[0]/robots[0]@id · нет атрибута «id»",
            "OTHER,   Замечание · world[0]/robots[0]@id · нет атрибута «id»",
        )
        fun `should put the place between the severity and the finding in diagnostic line at`(
            severity: DiagnosticLineAtSeverity,
            expected: String,
        ) {
            assertEquals(
                expected,
                l.pageDeveloper.diagnosticLineAt(severity = severity, place = "world[0]/robots[0]@id", finding = "нет атрибута «id»"),
            )
        }

        @Test
        fun `should quote the tag in diagnostic finding unknown element`() {
            assertEquals("неизвестный элемент «robot»", l.pageDeveloper.diagnosticFindingUnknownElement(tag = "robot"))
        }

        @Test
        fun `should show the details verbatim in diagnostic finding malformed xml`() {
            assertEquals(
                "некорректный XML: unexpected end of file",
                l.pageDeveloper.diagnosticFindingMalformedXml(details = "unexpected end of file"),
            )
        }

        @Test
        fun `should quote the tag in diagnostic finding missing child`() {
            assertEquals("нет вложенного элемента «world»", l.pageDeveloper.diagnosticFindingMissingChild(tag = "world"))
        }

        @Test
        fun `should quote the attribute in diagnostic finding missing attribute`() {
            assertEquals("нет атрибута «id»", l.pageDeveloper.diagnosticFindingMissingAttribute(attribute = "id"))
        }

        @Test
        fun `should quote the attribute and the actual value in diagnostic finding invalid attribute value`() {
            assertEquals(
                "атрибут «value» равен «abc», ожидается целое число",
                l.pageDeveloper.diagnosticFindingInvalidAttributeValue(attribute = "value", actual = "abc", expected = "целое число"),
            )
        }

        @Test
        fun `should keep large counts ungrouped in diagnostic finding invalid child count`() {
            assertEquals(
                "вложенных элементов 2147483647, ожидается 1412",
                l.pageDeveloper.diagnosticFindingInvalidChildCount(actual = Int.MAX_VALUE, expected = 1412),
            )
        }

        @Test
        fun `should state the missing limit in diagnostic finding missing time limit`() {
            assertEquals("не задано ограничение времени", l.pageDeveloper.diagnosticFindingMissingTimeLimit())
        }

        @Test
        fun `should keep a large count ungrouped in diagnostic finding multiple time limits`() {
            assertEquals(
                "ограничений времени 1412, ожидается одно",
                l.pageDeveloper.diagnosticFindingMultipleTimeLimits(count = 1412),
            )
        }

        @Test
        fun `should keep a negative value ungrouped in diagnostic finding negative time limit`() {
            assertEquals(
                "отрицательное ограничение времени -1412",
                l.pageDeveloper.diagnosticFindingNegativeTimeLimit(value = -1412L),
            )
        }

        @Test
        fun `should keep large values exact and ungrouped in diagnostic finding excessive time limit`() {
            assertEquals(
                "ограничение времени 9223372036854775807 больше допустимого 600000",
                l.pageDeveloper.diagnosticFindingExcessiveTimeLimit(value = Long.MAX_VALUE, maximum = 600_000L),
            )
        }

        @Test
        fun `should quote the event id in diagnostic finding invalid event id`() {
            assertEquals(
                "некорректный идентификатор события «finish»",
                l.pageDeveloper.diagnosticFindingInvalidEventId(id = "finish"),
            )
        }

        @Test
        fun `should capitalize the polygon term in diagnostic finding missing score output`() {
            assertEquals("Полигон не выводит балл", l.pageDeveloper.diagnosticFindingMissingScoreOutput())
        }

        @Test
        fun `should use the instrumental plural task and contest terms in guide`() {
            assertEquals(
                "Работайте с Задачами и Турами через вкладки Кабинета Разработчика.",
                l.pageDeveloper.guide(),
            )
        }

        @Test
        fun `should use the plural task term in tasks owned`() {
            assertEquals("Мои Задачи", l.pageDeveloper.tasksOwned())
        }

        @Test
        fun `should use the accusative plural community term in tasks shared`() {
            assertEquals("Доступные через Сообщества", l.pageDeveloper.tasksShared())
        }

        @Test
        fun `should use the genitive plural task term in tasks guide`() {
            assertEquals(
                "Список ваших Задач или Задач, доступных через Сообщества. Фильтры помогают найти нужную запись.",
                l.pageDeveloper.tasksGuide(),
            )
        }

        @Test
        fun `should use the genitive plural task term in tasks empty`() {
            assertEquals("Задач пока нет", l.pageDeveloper.tasksEmpty())
        }

        @Test
        fun `should use the accusative task term in tasks create`() {
            assertEquals("Создать Задачу", l.pageDeveloper.tasksCreate())
        }

        @Test
        fun `should agree the adjective with the task term in tasks dialog title`() {
            assertEquals("Новая Задача", l.pageDeveloper.tasksDialogTitle())
        }

        @Test
        fun `should use the task term in tasks created`() {
            assertEquals("Задача создана", l.pageDeveloper.tasksCreated())
        }

        @Test
        fun `should use the genitive plural and accusative contest terms in contests guide`() {
            assertEquals(
                "Список Туров и их параметры. Выберите строку, чтобы открыть Тур.",
                l.pageDeveloper.contestsGuide(),
            )
        }

        @Test
        fun `should use the plural task term in contests tasks`() {
            assertEquals("Задачи", l.pageDeveloper.contestsTasks())
        }

        @Test
        fun `should name the sharing column in contests shared`() {
            assertEquals("Доступ предоставлен", l.pageDeveloper.contestsShared())
        }

        @Test
        fun `should say yes in contests shared yes`() {
            assertEquals("Да", l.pageDeveloper.contestsSharedYes())
        }

        @Test
        fun `should say no in contests shared no`() {
            assertEquals("Нет", l.pageDeveloper.contestsSharedNo())
        }

        @Test
        fun `should use the genitive plural contest term in contests empty`() {
            assertEquals("Туров пока нет", l.pageDeveloper.contestsEmpty())
        }

        @Test
        fun `should use the accusative contest term in contests empty hint`() {
            assertEquals("Создайте Тур.", l.pageDeveloper.contestsEmptyHint())
        }

        @Test
        fun `should use the accusative contest term in contests create`() {
            assertEquals("Создать Тур", l.pageDeveloper.contestsCreate())
        }

        @Test
        fun `should agree the adjective with the contest term in contests dialog title`() {
            assertEquals("Новый Тур", l.pageDeveloper.contestsDialogTitle())
        }

        @Test
        fun `should ask for the version in contests version required`() {
            assertEquals("Выберите версию", l.pageDeveloper.contestsVersionRequired())
        }

        @Test
        fun `should use the genitive contest term in contests limit exceeds`() {
            assertEquals("Лимит больше длительности Тура", l.pageDeveloper.contestsLimitExceeds())
        }

        @Test
        fun `should use the contest term in contests created`() {
            assertEquals("Тур создан", l.pageDeveloper.contestsCreated())
        }

        @Test
        fun `should use the contest term in contest tab title`() {
            assertEquals("Тур", l.pageDeveloper.contestTabTitle())
        }

        @Test
        fun `should quote the contest name in contest title`() {
            assertEquals("Тур «Весенний»", l.pageDeveloper.contestTitle(name = "Весенний"))
        }

        @Test
        fun `should use the accusative contest term in contest delete`() {
            assertEquals("Удалить Тур", l.pageDeveloper.contestDelete())
        }

        @Test
        fun `should decline the contest task and community terms in contest guide`() {
            assertEquals(
                "Здесь показаны параметры Тура, состав Задач и доступ Сообществ.",
                l.pageDeveloper.contestGuide(),
            )
        }

        @Test
        fun `should use the contest term in contest locked`() {
            assertEquals("Тур нельзя изменить", l.pageDeveloper.contestLocked())
        }

        @Test
        fun `should decline the contest community and task terms in contest locked shared`() {
            assertEquals(
                "Доступ к Туру предоставлен Сообществам, поэтому Тур нельзя изменить или удалить, а Задачи — " +
                    "прикрепить к нему или открепить.",
                l.pageDeveloper.contestLockedShared(),
            )
        }

        @Test
        fun `should use the accusative contest term in contest locked not owner`() {
            assertEquals("Изменять Тур может только его владелец.", l.pageDeveloper.contestLockedNotOwner())
        }

        @Test
        fun `should use the genitive contest term in contest details guide`() {
            assertEquals(
                "Нажмите «Изменить», чтобы настроить параметры Тура, затем сохраните изменения или отмените их.",
                l.pageDeveloper.contestDetailsGuide(),
            )
        }

        @Test
        fun `should use the genitive contest term in contest details guide locked`() {
            assertEquals(
                "Параметры Тура доступны для просмотра. Причина ограничения изменений указана над блоками.",
                l.pageDeveloper.contestDetailsGuideLocked(),
            )
        }

        @Test
        fun `should use the contest term in contest edited`() {
            assertEquals("Тур изменён", l.pageDeveloper.contestEdited())
        }

        @Test
        fun `should use the genitive contest term in contest limit exceeds`() {
            assertEquals("Индивидуальный лимит больше длительности Тура", l.pageDeveloper.contestLimitExceeds())
        }

        @Test
        fun `should show the minutes in contest attempt minutes`() {
            assertEquals("90 мин", l.pageDeveloper.contestAttemptMinutes(minutes = 90L))
        }

        @Test
        fun `should keep large minutes exact and ungrouped in contest attempt minutes`() {
            assertEquals("9223372036854775807 мин", l.pageDeveloper.contestAttemptMinutes(minutes = Long.MAX_VALUE))
        }

        @Test
        fun `should say no in contest no attempt limit`() {
            assertEquals("Нет", l.pageDeveloper.contestNoAttemptLimit())
        }

        @Test
        fun `should use the plural community and dative contest terms in contest access guide`() {
            assertEquals(
                "Сообщества, которым предоставлен доступ к этому Туру.",
                l.pageDeveloper.contestAccessGuide(),
            )
        }

        @Test
        fun `should use the genitive plural task term in contest tasks guide`() {
            assertEquals("Список Задач и их основные сведения.", l.pageDeveloper.contestTasksGuide())
        }

        @Test
        fun `should quote the task name after the instrumental task term in contest task actions`() {
            assertEquals("Действия с Задачей «Лабиринт»", l.pageDeveloper.contestTaskActions(name = "Лабиринт"))
        }

        @Test
        fun `should use the task and genitive contest terms in contest task detached`() {
            assertEquals("Задача откреплена от Тура", l.pageDeveloper.contestTaskDetached())
        }

        @Test
        fun `should use the accusative task term in contest tasks empty hint`() {
            assertEquals(
                "Прикрепите Задачу, у которой есть зафиксированная версия.",
                l.pageDeveloper.contestTasksEmptyHint(),
            )
        }

        @Test
        fun `should use the accusative task term in contest attach task`() {
            assertEquals("Прикрепить Задачу", l.pageDeveloper.contestAttachTask())
        }

        @Test
        fun `should use the genitive task term in contest attach title`() {
            assertEquals("Прикрепление Задачи", l.pageDeveloper.contestAttachTitle())
        }

        @Test
        fun `should use the task term in contest attach lookup`() {
            assertEquals("Задача", l.pageDeveloper.contestAttachLookup())
        }

        @Test
        fun `should use the prepositional contest and genitive task terms in contest attach hint`() {
            assertEquals(
                "В Туре используется последняя зафиксированная версия Задачи",
                l.pageDeveloper.contestAttachHint(),
            )
        }

        @Test
        fun `should use the task and dative contest terms in contest attach done`() {
            assertEquals("Задача прикреплена к Туру", l.pageDeveloper.contestAttachDone())
        }

        @Test
        fun `should use the accusative task term in contest attach failed`() {
            assertEquals("Не удалось прикрепить Задачу", l.pageDeveloper.contestAttachFailed())
        }

        @Test
        fun `should decline the task term in contest attach not committed`() {
            assertEquals(
                "У Задачи нет зафиксированной версии. Сначала протестируйте и зафиксируйте Задачу.",
                l.pageDeveloper.contestAttachNotCommitted(),
            )
        }

        @Test
        fun `should use the task and genitive contest terms in contest attach version not supported`() {
            assertEquals(
                "Задача не поддерживает версию TRIK Studio этого Тура.",
                l.pageDeveloper.contestAttachVersionNotSupported(),
            )
        }

        @Test
        fun `should use the task and dative contest terms in contest attach already attached`() {
            assertEquals("Эта Задача уже прикреплена к Туру.", l.pageDeveloper.contestAttachAlreadyAttached())
        }

        @Test
        fun `should use the dative contest and plural task terms in contest attach shared`() {
            assertEquals(
                "Доступ к Туру уже предоставлен. Его Задачи нельзя изменить.",
                l.pageDeveloper.contestAttachShared(),
            )
        }

        @Test
        fun `should use the dative contest term in contest share title`() {
            assertEquals("Доступ к Туру", l.pageDeveloper.contestShareTitle())
        }

        @Test
        fun `should use the dative contest term in contest share confirm title`() {
            assertEquals("Предоставить доступ к Туру?", l.pageDeveloper.contestShareConfirmTitle())
        }

        @Test
        fun `should use the accusative contest term in contest share confirm text`() {
            assertEquals(
                "Отозвать доступ нельзя. После этого Тур нельзя будет изменить и удалить.",
                l.pageDeveloper.contestShareConfirmText(),
            )
        }

        @Test
        fun `should use the dative contest term in contest share done`() {
            assertEquals("Доступ к Туру предоставлен", l.pageDeveloper.contestShareDone())
        }

        @Test
        fun `should quote the contest name in contest delete confirm title`() {
            assertEquals("Удалить Тур «Весенний»?", l.pageDeveloper.contestDeleteConfirmTitle(name = "Весенний"))
        }

        @Test
        fun `should decline the contest and task terms in contest delete confirm text`() {
            assertEquals(
                "Тур удалится вместе со списком его Задач. Сами Задачи не изменятся.",
                l.pageDeveloper.contestDeleteConfirmText(),
            )
        }

        @Test
        fun `should name the deleting action in contest delete confirm action`() {
            assertEquals("Удалить", l.pageDeveloper.contestDeleteConfirmAction())
        }

        @Test
        fun `should use the contest term in contest deleted`() {
            assertEquals("Тур удалён", l.pageDeveloper.contestDeleted())
        }

        @Test
        fun `should use the task term in task tab title`() {
            assertEquals("Задача", l.pageDeveloper.taskTabTitle())
        }

        @Test
        fun `should name the action in task view committed`() {
            assertEquals("Посмотреть зафиксированную версию", l.pageDeveloper.taskViewCommitted())
        }

        @Test
        fun `should use the accusative task and plural resource terms in task guide`() {
            assertEquals(
                "Настройте Задачу, её Ресурсы и доступ, затем проверьте результаты тестирования.",
                l.pageDeveloper.taskGuide(),
            )
        }

        @Test
        fun `should name the committed version in task committed title`() {
            assertEquals("Зафиксированная версия", l.pageDeveloper.taskCommittedTitle())
        }

        @Test
        fun `should use the task term in task edited`() {
            assertEquals("Задача изменена", l.pageDeveloper.taskEdited())
        }

        @Test
        fun `should use the plural community term in task access guide`() {
            assertEquals(
                "Сообщества, которым предоставлен доступ. Управляйте доступом действиями этого блока.",
                l.pageDeveloper.taskAccessGuide(),
            )
        }

        @Test
        fun `should use the plural community term in task access empty hint`() {
            assertEquals(
                "Сообщества получат доступ к последней зафиксированной версии.",
                l.pageDeveloper.taskAccessEmptyHint(),
            )
        }

        @Test
        fun `should use the plural resource term in task resources title`() {
            assertEquals("Ресурсы", l.pageDeveloper.taskResourcesTitle())
        }

        @Test
        fun `should list the resource type terms in task resources subtitle`() {
            assertEquals("Условие, Упражнения, Полигоны и Авторские Решения", l.pageDeveloper.taskResourcesSubtitle())
        }

        @Test
        fun `should list the resource type terms in task resources guide`() {
            assertEquals(
                "Файлы Задачи: Условие, Упражнения, Полигоны и Авторские Решения. Откройте Ресурс для просмотра " +
                    "его версий.",
                l.pageDeveloper.taskResourcesGuide(),
            )
        }

        @Test
        fun `should quote the resource name after the instrumental resource term in task resources actions`() {
            assertEquals("Действия с Ресурсом «maze.xml»", l.pageDeveloper.taskResourcesActions(name = "maze.xml"))
        }

        @Test
        fun `should use the resource term in task resources detached`() {
            assertEquals("Ресурс откреплён", l.pageDeveloper.taskResourcesDetached())
        }

        @Test
        fun `should use the genitive plural resource term in task resources empty`() {
            assertEquals("Ресурсов пока нет", l.pageDeveloper.taskResourcesEmpty())
        }

        @Test
        fun `should list the accusative resource type terms in task resources empty hint`() {
            assertEquals(
                "Загрузите Условие, Упражнения, Полигоны и Авторские Решения.",
                l.pageDeveloper.taskResourcesEmptyHint(),
            )
        }

        @Test
        fun `should use the plural resource term in task resources not found`() {
            assertEquals("Ресурсы не найдены", l.pageDeveloper.taskResourcesNotFound())
        }

        @Test
        fun `should suggest changing the filters in task resources not found hint`() {
            assertEquals("Измените или сбросьте фильтры.", l.pageDeveloper.taskResourcesNotFoundHint())
        }

        @Test
        fun `should use the accusative plural resource term in task resources upload`() {
            assertEquals("Загрузить Ресурсы", l.pageDeveloper.taskResourcesUpload())
        }

        @Test
        fun `should name the usage filter in task resources usage`() {
            assertEquals("Использование", l.pageDeveloper.taskResourcesUsage())
        }

        @ParameterizedTest
        @CsvSource(
            "ALL,          Все",
            "WORKING,      В рабочей версии",
            "COMMITTED,    В зафиксированной версии",
            "ANY_REVISION, В любой версии",
            "UNATTACHED,   Нигде не прикреплены",
            "OTHER,        Неизвестное использование",
        )
        fun `should name the usage filter item in task resources usage value`(usage: TaskResourcesUsageValueUsage, expected: String) {
            assertEquals(expected, l.pageDeveloper.taskResourcesUsageValue(usage = usage))
        }

        @Test
        fun `should name the comparison in task resources differences`() {
            assertEquals("Различия версий", l.pageDeveloper.taskResourcesDifferences())
        }

        @Test
        fun `should use the plural resource term in task resources differences hint`() {
            assertEquals(
                "Различия версий: добавленные, откреплённые или заменённые Ресурсы.",
                l.pageDeveloper.taskResourcesDifferencesHint(),
            )
        }

        @Test
        fun `should use the genitive task term in task resources comparison unavailable`() {
            assertEquals(
                "Для сравнения нужны рабочая и зафиксированная версии Задачи.",
                l.pageDeveloper.taskResourcesComparisonUnavailable(),
            )
        }

        @Test
        fun `should name the testing block in task testing title`() {
            assertEquals("Тестирование", l.pageDeveloper.taskTestingTitle())
        }

        @Test
        fun `should use the genitive task and plural diagnostic terms in task testing guide`() {
            assertEquals(
                "История проверок Задачи. Откройте запись, чтобы увидеть Диагностики и результаты.",
                l.pageDeveloper.taskTestingGuide(),
            )
        }

        @Test
        fun `should state that there was no testing in task testing empty`() {
            assertEquals("Тестирований пока не было", l.pageDeveloper.taskTestingEmpty())
        }

        @Test
        fun `should suggest testing the working version in task testing empty hint`() {
            assertEquals(
                "Протестируйте рабочую версию, чтобы её можно было зафиксировать.",
                l.pageDeveloper.taskTestingEmptyHint(),
            )
        }

        @Test
        fun `should name the testing action in task testing run`() {
            assertEquals("Протестировать", l.pageDeveloper.taskTestingRun())
        }

        @Test
        fun `should state the failure in task testing run failed`() {
            assertEquals("Не удалось запустить тестирование", l.pageDeveloper.taskTestingRunFailed())
        }

        @Test
        fun `should state the running testing in task testing running`() {
            assertEquals("Тестирование уже идёт", l.pageDeveloper.taskTestingRunning())
        }

        @Test
        fun `should ask to wait in task testing running hint`() {
            assertEquals("Дождитесь результата текущего запроса.", l.pageDeveloper.taskTestingRunningHint())
        }

        @Test
        fun `should state the start in task testing started`() {
            assertEquals("Тестирование запущено", l.pageDeveloper.taskTestingStarted())
        }

        @Test
        fun `should point to the history in task testing started hint`() {
            assertEquals("Результат появится в истории тестирований.", l.pageDeveloper.taskTestingStartedHint())
        }

        @Test
        fun `should name the commit action in task commit`() {
            assertEquals("Зафиксировать", l.pageDeveloper.taskCommit())
        }

        @Test
        fun `should use the accusative task term in task commit title`() {
            assertEquals("Зафиксировать Задачу?", l.pageDeveloper.taskCommitTitle())
        }

        @Test
        fun `should decline the task and contest terms in task commit text`() {
            assertEquals(
                "Задача перейдёт в состояние «Зафиксирована». Туры с Задачей начнут использовать новую версию.",
                l.pageDeveloper.taskCommitText(),
            )
        }

        @Test
        fun `should use the accusative plural submission term in task commit regrade`() {
            assertEquals("Перепроверить Посылки", l.pageDeveloper.taskCommitRegrade())
        }

        @Test
        fun `should decline the submission contest and polygon terms in task commit regrade hint`() {
            assertEquals(
                "Посылки в Турах будут проверены заново, если изменился набор Полигонов.",
                l.pageDeveloper.taskCommitRegradeHint(),
            )
        }

        @Test
        fun `should use the accusative task term in task commit failed`() {
            assertEquals("Не удалось зафиксировать Задачу", l.pageDeveloper.taskCommitFailed())
        }

        @Test
        fun `should use the task term in task committed`() {
            assertEquals("Задача зафиксирована", l.pageDeveloper.taskCommitted())
        }

        @Test
        fun `should name the revert action in task revert`() {
            assertEquals("Откатить", l.pageDeveloper.taskRevert())
        }

        @Test
        fun `should use the genitive task term in task revert title`() {
            assertEquals("Откатить изменения Задачи?", l.pageDeveloper.taskRevertTitle())
        }

        @Test
        fun `should use the task term in task revert text`() {
            assertEquals(
                "Задача вернётся к последней зафиксированной версии и перейдёт в состояние «Зафиксирована». " +
                    "Незафиксированные изменения пропадут.",
                l.pageDeveloper.taskRevertText(),
            )
        }

        @Test
        fun `should use the genitive task term in task reverted`() {
            assertEquals("Изменения Задачи отменены", l.pageDeveloper.taskReverted())
        }

        @Test
        fun `should use the dative task term in task share title`() {
            assertEquals("Доступ к Задаче", l.pageDeveloper.taskShareTitle())
        }

        @Test
        fun `should use the dative task term in task share confirm title`() {
            assertEquals("Предоставить доступ к Задаче?", l.pageDeveloper.taskShareConfirmTitle())
        }

        @Test
        fun `should use the plural community and dative task terms in task share confirm text`() {
            assertEquals(
                "Отозвать доступ нельзя: Сообщества сохранят доступ к Задаче.",
                l.pageDeveloper.taskShareConfirmText(),
            )
        }

        @Test
        fun `should use the dative task term in task share done`() {
            assertEquals("Доступ к Задаче предоставлен", l.pageDeveloper.taskShareDone())
        }

        @Test
        fun `should use the genitive plural resource term in task upload title`() {
            assertEquals("Загрузка Ресурсов", l.pageDeveloper.taskUploadTitle())
        }

        @ParameterizedTest
        @CsvSource(
            "1,    10,   'До 1 файла, каждый до 10 МиБ'",
            "2,    10,   'До 2 файлов, каждый до 10 МиБ'",
            "5,    10,   'До 5 файлов, каждый до 10 МиБ'",
            "20,   10,   'До 20 файлов, каждый до 10 МиБ'",
            "21,   10,   'До 21 файла, каждый до 10 МиБ'",
            "1412, 2048, 'До 1412 файлов, каждый до 2048 МиБ'",
        )
        fun `should agree the genitive file count and keep numbers ungrouped in task upload subtitle`(
            maxFiles: Int,
            maxMebibytes: Int,
            expected: String,
        ) {
            assertEquals(expected, l.pageDeveloper.taskUploadSubtitle(maxFiles = maxFiles, maxMebibytes = maxMebibytes))
        }

        @Test
        fun `should name the files in task upload files`() {
            assertEquals("Файлы", l.pageDeveloper.taskUploadFiles())
        }

        @Test
        fun `should name the next action in task upload next`() {
            assertEquals("Далее", l.pageDeveloper.taskUploadNext())
        }

        @Test
        fun `should use the plural resource term in task describe title`() {
            assertEquals("Новые Ресурсы", l.pageDeveloper.taskDescribeTitle())
        }

        @Test
        fun `should use the plural resource and accusative task terms in task describe subtitle`() {
            assertEquals("Ресурсы загрузятся в Задачу без прикрепления", l.pageDeveloper.taskDescribeSubtitle())
        }

        @Test
        fun `should number the resource and show the file name in task describe section`() {
            assertEquals("Ресурс 1 · maze.xml", l.pageDeveloper.taskDescribeSection(number = 1, fileName = "maze.xml"))
        }

        @Test
        fun `should keep a large number ungrouped in task describe section`() {
            assertEquals("Ресурс 1412 · maze.xml", l.pageDeveloper.taskDescribeSection(number = 1412, fileName = "maze.xml"))
        }

        @Test
        fun `should ask for the type in task describe type required`() {
            assertEquals("Выберите тип", l.pageDeveloper.taskDescribeTypeRequired())
        }

        @Test
        fun `should name the upload action in task describe submit`() {
            assertEquals("Загрузить", l.pageDeveloper.taskDescribeSubmit())
        }

        @Test
        fun `should use the plural resource term in task describe done`() {
            assertEquals("Ресурсы загружены", l.pageDeveloper.taskDescribeDone())
        }

        @Test
        fun `should keep a large count ungrouped in task total`() {
            assertEquals("Всего: 1412.", l.pageDeveloper.taskTotal(count = 1412))
        }

        @Test
        fun `should use the genitive plural resource term in task attach title`() {
            assertEquals("Прикрепление Ресурсов", l.pageDeveloper.taskAttachTitle())
        }

        @Test
        fun `should use the genitive resource term in task attach hint`() {
            assertEquals("Прикрепляется последняя версия Ресурса", l.pageDeveloper.taskAttachHint())
        }

        @Test
        fun `should use the plural resource term in task attach done`() {
            assertEquals("Ресурсы прикреплены", l.pageDeveloper.taskAttachDone())
        }

        @ParameterizedTest
        @CsvSource(
            "NEW,         Задача перейдёт в состояние «Новая»",
            "UNCOMMITTED, Задача перейдёт в состояние «Не зафиксирована»",
            "COMMITTED,   Задача перейдёт в состояние «Зафиксирована»",
            "OTHER,       Задача перейдёт в другое состояние",
        )
        fun `should quote the target state in task state change title`(state: TaskStateChangeTitleState, expected: String) {
            assertEquals(expected, l.pageDeveloper.taskStateChangeTitle(state = state))
        }

        @Test
        fun `should decline the contest and task terms in task state change text`() {
            assertEquals(
                "Изменения попадут в рабочую версию. Туры продолжат использовать последнюю зафиксированную версию, " +
                    "пока Задачу не зафиксируют снова.",
                l.pageDeveloper.taskStateChangeText(),
            )
        }

        @Test
        fun `should name the continue action in task state change action`() {
            assertEquals("Продолжить", l.pageDeveloper.taskStateChangeAction())
        }

        @Test
        fun `should use the resource term in resource tab title`() {
            assertEquals("Ресурс", l.pageDeveloper.resourceTabTitle())
        }

        @ParameterizedTest
        @CsvSource(
            "STATEMENT,          Условие «maze.xml»",
            "EXERCISE,           Упражнение «maze.xml»",
            "TEST,               Полигон «maze.xml»",
            "DEVELOPER_SOLUTION, Авторское Решение «maze.xml»",
            "OTHER,              Ресурс «maze.xml»",
        )
        fun `should quote the name after the resource type term in resource title`(type: ResourceTitleType, expected: String) {
            assertEquals(expected, l.pageDeveloper.resourceTitle(type = type, name = "maze.xml"))
        }

        @Test
        fun `should decline the resource and developer solution terms in resource guide`() {
            assertEquals(
                "Просматривайте сведения о Ресурсе и историю его версий. Изменение только названия сохраняется " +
                    "в последней версии. Новый файл или изменение ожидаемого балла Авторского Решения создаёт новую " +
                    "версию Ресурса.",
                l.pageDeveloper.resourceGuide(),
            )
        }

        @Test
        fun `should name the latest version id in resource latest id`() {
            assertEquals("ID последней версии", l.pageDeveloper.resourceLatestId())
        }

        @Test
        fun `should name the working version id in resource working id`() {
            assertEquals("ID в рабочей версии", l.pageDeveloper.resourceWorkingId())
        }

        @Test
        fun `should name the committed version id in resource committed id`() {
            assertEquals("ID в зафиксированной версии", l.pageDeveloper.resourceCommittedId())
        }

        @Test
        fun `should state the missing working version in resource no working`() {
            assertEquals("Нет рабочей версии", l.pageDeveloper.resourceNoWorking())
        }

        @Test
        fun `should agree with the feminine version in resource not committed`() {
            assertEquals("Не зафиксирована", l.pageDeveloper.resourceNotCommitted())
        }

        @Test
        fun `should agree with the masculine resource in resource not attached`() {
            assertEquals("Не прикреплён", l.pageDeveloper.resourceNotAttached())
        }

        @Test
        fun `should name the new file in resource new file`() {
            assertEquals("Новый файл", l.pageDeveloper.resourceNewFile())
        }

        @Test
        fun `should head the history block in resource history title`() {
            assertEquals("История версий", l.pageDeveloper.resourceHistoryTitle())
        }

        @Test
        fun `should use the genitive resource term in resource history guide`() {
            assertEquals(
                "Сохранённые версии Ресурса. Для каждой версии можно скачать её файл.",
                l.pageDeveloper.resourceHistoryGuide(),
            )
        }

        @Test
        fun `should name the change time in resource history changed at`() {
            assertEquals("Изменено", l.pageDeveloper.resourceHistoryChangedAt())
        }

        @Test
        fun `should use the resource term in resource edited`() {
            assertEquals("Ресурс изменён", l.pageDeveloper.resourceEdited())
        }

        @Test
        fun `should use the accusative resource term in resource change title`() {
            assertEquals("Изменить прикреплённый Ресурс?", l.pageDeveloper.resourceChangeTitle())
        }

        @Test
        fun `should use the genitive resource and task terms in resource change text`() {
            assertEquals(
                "Появится новая версия Ресурса, и у Задачи появятся незафиксированные изменения.",
                l.pageDeveloper.resourceChangeText(),
            )
        }

        @Test
        fun `should add the state change in resource change text uncommitted`() {
            assertEquals(
                "Появится новая версия Ресурса, и у Задачи появятся незафиксированные изменения. Задача перейдёт " +
                    "в состояние «Не зафиксирована».",
                l.pageDeveloper.resourceChangeTextUncommitted(),
            )
        }

        @Test
        fun `should name the change action in resource change action`() {
            assertEquals("Изменить", l.pageDeveloper.resourceChangeAction())
        }

        @Test
        fun `should use the genitive task term in testing tab title`() {
            assertEquals("Тестирование Задачи", l.pageDeveloper.testingTabTitle())
        }

        @Test
        fun `should show the start in the context zone with 24 hours in testing title`() {
            assertEquals(
                "Тестирование от 12.10.2026 14:05",
                l.pageDeveloper.testingTitle(startedAt = Instant.parse("2026-10-12T11:05:00Z")),
            )
        }

        @Test
        fun `should show the start in the context zone in testing started at value`() {
            assertEquals("12.10.2026 14:05", l.pageDeveloper.testingStartedAtValue(startedAt = Instant.parse("2026-10-12T11:05:00Z")))
        }

        @Test
        fun `should pad the day month and hour with zeros in testing started at value`() {
            assertEquals("05.03.2026 09:07", l.pageDeveloper.testingStartedAtValue(startedAt = Instant.parse("2026-03-05T06:07:00Z")))
        }

        @Test
        fun `should decline the task diagnostic and submission terms in testing guide`() {
            assertEquals(
                "Здесь показаны состояние тестирования Задачи, Диагностики и результаты Посылок.",
                l.pageDeveloper.testingGuide(),
            )
        }

        @Test
        fun `should describe the request in testing details guide`() {
            assertEquals(
                "Параметры выбранного запроса тестирования и состояние его выполнения.",
                l.pageDeveloper.testingDetailsGuide(),
            )
        }

        @Test
        fun `should use the plural diagnostic term in testing diagnostics title`() {
            assertEquals("Диагностики", l.pageDeveloper.testingDiagnosticsTitle())
        }

        @Test
        fun `should use the genitive plural polygon term in testing diagnostics guide`() {
            assertEquals(
                "Замечания проверки Полигонов из выбранного тестирования.",
                l.pageDeveloper.testingDiagnosticsGuide(),
            )
        }

        @Test
        fun `should use the polygon term in testing diagnostics polygon`() {
            assertEquals("Полигон", l.pageDeveloper.testingDiagnosticsPolygon())
        }

        @Test
        fun `should name the remarks in testing diagnostics remarks`() {
            assertEquals("Замечания", l.pageDeveloper.testingDiagnosticsRemarks())
        }

        @Test
        fun `should state no remarks in testing diagnostics no remarks`() {
            assertEquals("Без замечаний", l.pageDeveloper.testingDiagnosticsNoRemarks())
        }

        @Test
        fun `should use the genitive plural diagnostic term in testing diagnostics empty`() {
            assertEquals("Диагностик пока нет", l.pageDeveloper.testingDiagnosticsEmpty())
        }

        @Test
        fun `should use the genitive plural diagnostic term in testing diagnostics empty hint`() {
            assertEquals(
                "Результаты появятся после выполнения Диагностик.",
                l.pageDeveloper.testingDiagnosticsEmptyHint(),
            )
        }

        @Test
        fun `should prefix the id with a number sign in testing unknown resource`() {
            assertEquals("#42", l.pageDeveloper.testingUnknownResource(id = 42L))
        }

        @Test
        fun `should keep the largest id exact and ungrouped in testing unknown resource`() {
            assertEquals("#9223372036854775807", l.pageDeveloper.testingUnknownResource(id = Long.MAX_VALUE))
        }

        @Test
        fun `should use the plural submission term in testing submissions guide`() {
            assertEquals("Посылки и результаты их проверки.", l.pageDeveloper.testingSubmissionsGuide())
        }

        @Test
        fun `should use the developer solution term in testing submissions developer solution`() {
            assertEquals("Авторское Решение", l.pageDeveloper.testingSubmissionsDeveloperSolution())
        }

        @ParameterizedTest
        @CsvSource(
            "NO_RESULT,      Без результата",
            "CHECKING,       Проверяется",
            "SCORE_MISMATCH, Другой балл",
            "GRADING_FAILED, Ошибка проверки",
            "EXPECTED_SCORE, Ожидаемый балл",
            "OTHER,          Неизвестный статус",
        )
        fun `should name the submission status in testing submissions status`(status: TestingSubmissionsStatusStatus, expected: String) {
            assertEquals(expected, l.pageDeveloper.testingSubmissionsStatus(status = status))
        }

        @Test
        fun `should show the actual and expected scores in testing submissions score`() {
            assertEquals("7 / 10", l.pageDeveloper.testingSubmissionsScore(actual = 7L, expected = 10))
        }

        @Test
        fun `should keep large scores exact and ungrouped in testing submissions score`() {
            assertEquals(
                "9223372036854775807 / 2147483647",
                l.pageDeveloper.testingSubmissionsScore(actual = Long.MAX_VALUE, expected = Int.MAX_VALUE),
            )
        }

        @Test
        fun `should show a dash for the actual score in testing submissions score pending`() {
            assertEquals("— / 1412", l.pageDeveloper.testingSubmissionsScorePending(expected = 1412))
        }
    }

    private companion object {
        // ICU separates digit groups with a no-break space; the code point keeps the expectations visible.
        val NBSP = Char(0x00A0).toString()
    }
}
