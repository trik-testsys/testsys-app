package tech.testsys.infra.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.infra.localization.PageJudge.SolutionLanguageLanguage
import tech.testsys.infra.localization.PageJudge.SolutionStatusStatus
import tech.testsys.infra.localization.UiTransfer.UploadLimitsUnit
import tech.testsys.infra.localization.bundle.SupportedRegion
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
    inner class PageManagerTests {

        @Test
        fun `should name the manager cabinet in title`() {
            assertEquals("Кабинет Организатора", l.pageManager.title())
        }

        @Test
        fun `should use the accusative plural class and competition terms in guide`() {
            assertEquals(
                "Создавайте Классы и Соревнования и открывайте их через таблицы соответствующих вкладок.",
                l.pageManager.guide(),
            )
        }

        @Test
        fun `should name the identifier in id`() {
            assertEquals("ID", l.pageManager.id())
        }

        @Test
        fun `should name the name in name`() {
            assertEquals("Название", l.pageManager.name())
        }

        @Test
        fun `should name the nickname in nickname`() {
            assertEquals("Псевдоним", l.pageManager.nickname())
        }

        @Test
        fun `should name the last login in last login`() {
            assertEquals("Последний вход", l.pageManager.lastLogin())
        }

        @Test
        fun `should name the description in description`() {
            assertEquals("Описание", l.pageManager.description())
        }

        @Test
        fun `should use the access code term in access code`() {
            assertEquals("Код-доступа", l.pageManager.accessCode())
        }

        @Test
        fun `should use the genitive plural student term in student count`() {
            assertEquals("Учеников", l.pageManager.studentCount())
        }

        @Test
        fun `should use the genitive plural participant term in participant count`() {
            assertEquals("Участников", l.pageManager.participantCount())
        }

        @Test
        fun `should use the genitive plural contest term in contest count`() {
            assertEquals("Туров", l.pageManager.contestCount())
        }

        @Test
        fun `should ask for the name in name required`() {
            assertEquals("Укажите название", l.pageManager.nameRequired())
        }

        @Test
        fun `should name the creating action in create`() {
            assertEquals("Создать", l.pageManager.create())
        }

        @Test
        fun `should name the csv download in download csv`() {
            assertEquals("Скачать CSV", l.pageManager.downloadCsv())
        }

        @Test
        fun `should name the start in starts at`() {
            assertEquals("Начало", l.pageManager.startsAt())
        }

        @Test
        fun `should name the end in ends at`() {
            assertEquals("Конец", l.pageManager.endsAt())
        }

        @Test
        fun `should name the attempt duration in attempt duration`() {
            assertEquals("Время на прохождение", l.pageManager.attemptDuration())
        }

        @Test
        fun `should head the details block in details title`() {
            assertEquals("Сведения", l.pageManager.detailsTitle())
        }

        @Test
        fun `should quote the edit button in details guide`() {
            assertEquals(
                "Просмотрите параметры. Нажмите «Изменить», чтобы отредактировать их, затем сохраните изменения " +
                    "или отмените их.",
                l.pageManager.detailsGuide(),
            )
        }

        @Test
        fun `should use the genitive plural class term in classes guide`() {
            assertEquals("Список Классов и сведения о них.", l.pageManager.classesGuide())
        }

        @Test
        fun `should use the genitive plural class term in classes empty`() {
            assertEquals("Классов пока нет", l.pageManager.classesEmpty())
        }

        @Test
        fun `should decline the class, student and invite code terms in classes empty hint`() {
            assertEquals(
                "Создайте Класс и раздайте Ученикам Код-приглашение.",
                l.pageManager.classesEmptyHint(),
            )
        }

        @Test
        fun `should use the accusative class term in classes create`() {
            assertEquals("Создать Класс", l.pageManager.classesCreate())
        }

        @Test
        fun `should use the class term in classes dialog title`() {
            assertEquals("Новый Класс", l.pageManager.classesDialogTitle())
        }

        @Test
        fun `should use the class term in classes created`() {
            assertEquals("Класс создан", l.pageManager.classesCreated())
        }

        @Test
        fun `should use the genitive plural competition term in competitions guide`() {
            assertEquals("Список Соревнований и сведения о них.", l.pageManager.competitionsGuide())
        }

        @Test
        fun `should use the genitive plural competition term in competitions empty`() {
            assertEquals("Соревнований пока нет", l.pageManager.competitionsEmpty())
        }

        @Test
        fun `should decline the competition and participant terms in competitions empty hint`() {
            assertEquals(
                "Создайте Соревнование и добавьте в него Участников.",
                l.pageManager.competitionsEmptyHint(),
            )
        }

        @Test
        fun `should use the accusative competition term in competitions create`() {
            assertEquals("Создать Соревнование", l.pageManager.competitionsCreate())
        }

        @Test
        fun `should use the competition term in competitions dialog title`() {
            assertEquals("Новое Соревнование", l.pageManager.competitionsDialogTitle())
        }

        @Test
        fun `should use the competition term in competitions created`() {
            assertEquals("Соревнование создано", l.pageManager.competitionsCreated())
        }

        @Test
        fun `should describe the name match in filter name hint`() {
            assertEquals("Часть названия, без учёта регистра", l.pageManager.filterNameHint())
        }

        @Test
        fun `should name the creation date in filter created`() {
            assertEquals("Дата создания", l.pageManager.filterCreated())
        }

        @Test
        fun `should decline the contest term in contests guide`() {
            assertEquals(
                "Список Туров и их параметры. Выберите строку, чтобы открыть Тур.",
                l.pageManager.contestsGuide(),
            )
        }

        @Test
        fun `should use the genitive plural contest term in contests empty`() {
            assertEquals("Туров пока нет", l.pageManager.contestsEmpty())
        }

        @Test
        fun `should decline the contest and community terms in contests empty hint`() {
            assertEquals("Добавьте Тур, открытый вашим Сообществам.", l.pageManager.contestsEmptyHint())
        }

        @Test
        fun `should use the accusative contest term in contests add`() {
            assertEquals("Добавить Тур", l.pageManager.contestsAdd())
        }

        @Test
        fun `should use the genitive contest term in contests dialog title`() {
            assertEquals("Добавление Тура", l.pageManager.contestsDialogTitle())
        }

        @Test
        fun `should use the contest term in contests contest`() {
            assertEquals("Тур", l.pageManager.contestsContest())
        }

        @Test
        fun `should decline the contest and community terms in contests lookup hint`() {
            assertEquals("Туры, открытые вашим Сообществам", l.pageManager.contestsLookupHint())
        }

        @Test
        fun `should use the accusative contest term in contests required`() {
            assertEquals("Выберите Тур", l.pageManager.contestsRequired())
        }

        @Test
        fun `should name the adding action in contests submit`() {
            assertEquals("Добавить", l.pageManager.contestsSubmit())
        }

        @Test
        fun `should use the class term in class tab title`() {
            assertEquals("Класс", l.pageManager.classTabTitle())
        }

        @Test
        fun `should quote the class name in class title`() {
            assertEquals("Класс «7 «Б»»", l.pageManager.classTitle(name = "7 «Б»"))
        }

        @Test
        fun `should decline the class, student and contest terms in class guide`() {
            assertEquals(
                "Управляйте сведениями Класса, приглашением, Учениками и назначенными Турами.",
                l.pageManager.classGuide(),
            )
        }

        @Test
        fun `should use the class term in class saved`() {
            assertEquals("Класс изменён", l.pageManager.classSaved())
        }

        @Test
        fun `should decline the contest and class terms in class contest added`() {
            assertEquals("Тур добавлен в Класс", l.pageManager.classContestAdded())
        }

        @Test
        fun `should use the invite code term in class invite code`() {
            assertEquals("Код-приглашение", l.pageManager.classInviteCode())
        }

        @Test
        fun `should keep the anaphoric code reference lowercase in class invite guide`() {
            assertEquals(
                "Передайте этот код Ученикам для присоединения к Классу. Здесь же можно продлить срок его действия.",
                l.pageManager.classInviteGuide(),
            )
        }

        @Test
        fun `should name the expiry in class invite expires`() {
            assertEquals("Действует до", l.pageManager.classInviteExpires())
        }

        @Test
        fun `should name the copying action in class invite copy`() {
            assertEquals("Скопировать", l.pageManager.classInviteCopy())
        }

        @Test
        fun `should name the replacing action in class invite replace`() {
            assertEquals("Заменить", l.pageManager.classInviteReplace())
        }

        @Test
        fun `should use the accusative invite code term in class invite replace title`() {
            assertEquals("Заменить Код-приглашение?", l.pageManager.classInviteReplaceTitle())
        }

        @Test
        fun `should use the invite code term in class invite replace text`() {
            assertEquals("Прежний Код-приглашение перестанет действовать.", l.pageManager.classInviteReplaceText())
        }

        @Test
        fun `should use the invite code term in class invite replaced`() {
            assertEquals("Код-приглашение заменён", l.pageManager.classInviteReplaced())
        }

        @Test
        fun `should name the extending action in class invite extend`() {
            assertEquals("Продлить", l.pageManager.classInviteExtend())
        }

        @Test
        fun `should use the genitive invite code term in class invite extended`() {
            assertEquals("Срок действия Кода-приглашения продлён", l.pageManager.classInviteExtended())
        }

        @Test
        fun `should use the plural student term in class students title`() {
            assertEquals("Ученики", l.pageManager.classStudentsTitle())
        }

        @Test
        fun `should decline the student and class terms in class students guide`() {
            assertEquals(
                "Ученики, присоединившиеся к Классу, и время их входа.",
                l.pageManager.classStudentsGuide(),
            )
        }

        @Test
        fun `should use the genitive plural student term in class students empty`() {
            assertEquals("Учеников пока нет", l.pageManager.classStudentsEmpty())
        }

        @Test
        fun `should decline the student, class and invite code terms in class students empty hint`() {
            assertEquals(
                "Ученики присоединяются к Классу по Коду-приглашению.",
                l.pageManager.classStudentsEmptyHint(),
            )
        }

        @Test
        fun `should use the competition term in competition tab title`() {
            assertEquals("Соревнование", l.pageManager.competitionTabTitle())
        }

        @Test
        fun `should quote the competition name in competition title`() {
            assertEquals("Соревнование «Кубок»", l.pageManager.competitionTitle(name = "Кубок"))
        }

        @Test
        fun `should decline the competition, participant and contest terms in competition guide`() {
            assertEquals(
                "Управляйте Соревнованием, его Участниками и Турами.",
                l.pageManager.competitionGuide(),
            )
        }

        @Test
        fun `should use the competition term in competition saved`() {
            assertEquals("Соревнование изменено", l.pageManager.competitionSaved())
        }

        @Test
        fun `should decline the contest and competition terms in competition contest added`() {
            assertEquals("Тур добавлен в Соревнование", l.pageManager.competitionContestAdded())
        }

        @Test
        fun `should use the plural participant term in competition participants title`() {
            assertEquals("Участники", l.pageManager.competitionParticipantsTitle())
        }

        @Test
        fun `should decline the participant, competition and access code terms in competition participants guide`() {
            assertEquals(
                "Участники Соревнования, их Коды-доступа и сведения о входе.",
                l.pageManager.competitionParticipantsGuide(),
            )
        }

        @Test
        fun `should quote the nickname in competition participants actions`() {
            assertEquals(
                "Действия с Участником «Маша»",
                l.pageManager.competitionParticipantsActions(name = "Маша"),
            )
        }

        @Test
        fun `should name the deleting action in competition participants delete`() {
            assertEquals("Удалить", l.pageManager.competitionParticipantsDelete())
        }

        @Test
        fun `should use the genitive plural participant term in competition participants empty`() {
            assertEquals("Участников пока нет", l.pageManager.competitionParticipantsEmpty())
        }

        @Test
        fun `should decline the participant and access code terms in competition participants empty hint`() {
            assertEquals(
                "Создайте Участников, чтобы раздать им Коды-доступа.",
                l.pageManager.competitionParticipantsEmptyHint(),
            )
        }

        @Test
        fun `should use the accusative plural participant term in competition participants create`() {
            assertEquals("Создать Участников", l.pageManager.competitionParticipantsCreate())
        }

        @Test
        fun `should quote the nickname in competition participants delete title`() {
            assertEquals(
                "Удалить Участника «Маша»?",
                l.pageManager.competitionParticipantsDeleteTitle(name = "Маша"),
            )
        }

        @Test
        fun `should decline the access code, participant and submission terms in competition participants delete text`() {
            assertEquals(
                "Код-доступа Участника перестанет действовать. Участника с Посылками удалить нельзя.",
                l.pageManager.competitionParticipantsDeleteText(),
            )
        }

        @Test
        fun `should use the participant term in competition participants deleted`() {
            assertEquals("Участник удалён", l.pageManager.competitionParticipantsDeleted())
        }

        @Test
        fun `should use the plural participant term in competition participants dialog title`() {
            assertEquals("Новые Участники", l.pageManager.competitionParticipantsDialogTitle())
        }

        @Test
        fun `should decline the participant and access code terms in competition participants dialog subtitle`() {
            assertEquals(
                "Каждый Участник получит свой Код-доступа",
                l.pageManager.competitionParticipantsDialogSubtitle(),
            )
        }

        @Test
        fun `should name the amount in competition participants count`() {
            assertEquals("Количество", l.pageManager.competitionParticipantsCount())
        }

        @Test
        fun `should ask for the amount in competition participants count required`() {
            assertEquals("Укажите количество", l.pageManager.competitionParticipantsCountRequired())
        }

        @Test
        fun `should use the plural participant term in competition participants created`() {
            assertEquals("Участники созданы", l.pageManager.competitionParticipantsCreated())
        }

        @ParameterizedTest
        @CsvSource(
            "1,    Всего: 1.",
            "5,    Всего: 5.",
            "1000, Всего: 1000.",
        )
        fun `should show the ungrouped count in competition participants created total`(count: Int, expected: String) {
            assertEquals(expected, l.pageManager.competitionParticipantsCreatedTotal(count = count))
        }

        @Test
        fun `should use the contest term in contest tab title`() {
            assertEquals("Тур", l.pageManager.contestTabTitle())
        }

        @Test
        fun `should quote the contest name in contest title`() {
            assertEquals("Тур «Отборочный»", l.pageManager.contestTitle(name = "Отборочный"))
        }

        @Test
        fun `should use the genitive contest term in contest guide`() {
            assertEquals(
                "Просмотрите параметры Тура и результаты его прохождения.",
                l.pageManager.contestGuide(),
            )
        }

        @Test
        fun `should decline the contest, class and competition terms in contest details guide`() {
            assertEquals(
                "Параметры Тура, назначенного выбранному Классу или Соревнованию.",
                l.pageManager.contestDetailsGuide(),
            )
        }

        @Test
        fun `should name the results in contest results title`() {
            assertEquals("Результаты", l.pageManager.contestResultsTitle())
        }

        @Test
        fun `should use the genitive contest term in contest results guide`() {
            assertEquals(
                "Сводные результаты Тура по Задачам; их можно скачать в CSV.",
                l.pageManager.contestResultsGuide(),
            )
        }

        @Test
        fun `should keep the task id ungrouped in contest results task`() {
            assertEquals("Движение · 12345", l.pageManager.contestResultsTask(name = "Движение", id = 12_345L))
        }

        @Test
        fun `should keep the largest task id exact in contest results task`() {
            assertEquals(
                "Движение · 9223372036854775807",
                l.pageManager.contestResultsTask(name = "Движение", id = Long.MAX_VALUE),
            )
        }

        @ParameterizedTest
        @CsvSource(
            "42,   3,    42 · 3",
            "0,    1,    0 · 1",
            "-5,   2,    -5 · 2",
            "1000, 1000, 1000 · 1000",
        )
        fun `should show the ungrouped score and submission count in contest results cell`(score: Int, count: Int, expected: String) {
            assertEquals(expected, l.pageManager.contestResultsCell(score = score, count = count))
        }

        @ParameterizedTest
        @CsvSource(
            "0,    — · 0",
            "1000, — · 1000",
        )
        fun `should show a dash for the missing best score in contest results cell empty`(count: Int, expected: String) {
            assertEquals(expected, l.pageManager.contestResultsCellEmpty(count = count))
        }

        @Test
        fun `should render the plain no-members text in contest results empty`() {
            assertEquals("Участников нет", l.pageManager.contestResultsEmpty())
        }

        @Test
        fun `should use the plural student and participant terms in contest results empty hint`() {
            assertEquals(
                "Строки появятся, когда в группе будут Ученики или Участники.",
                l.pageManager.contestResultsEmptyHint(),
            )
        }

        @Test
        fun `should append the score column name to the task in contest csv score`() {
            assertEquals("Движение — балл", l.pageManager.contestCsvScore(task = "Движение"))
        }

        @Test
        fun `should use the genitive plural submission term in contest csv submissions`() {
            assertEquals("Движение — Посылок", l.pageManager.contestCsvSubmissions(task = "Движение"))
        }
    }

    private companion object {
        // ICU separates digit groups with a no-break space; the code point keeps the expectations visible.
        val NBSP = Char(0x00A0).toString()
    }
}
