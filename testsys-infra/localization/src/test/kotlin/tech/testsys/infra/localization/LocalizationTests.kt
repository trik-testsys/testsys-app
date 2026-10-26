package tech.testsys.infra.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.infra.localization.PageAdministrator.UserHeadKind
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.infra.localization.PageCabinet.ValidationStatusStatus
import tech.testsys.infra.localization.PageDeveloper.DiagnosticLineAtSeverity
import tech.testsys.infra.localization.PageDeveloper.DiagnosticLineSeverity
import tech.testsys.infra.localization.PageDeveloper.LanguageNameLanguage
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
                    "Никому не сообщайте этот код.\n\n" +
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
                    "Никому не сообщайте этот код.\n\n" +
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

        @Test
        fun `should name the confirmation code in access restoration confirmation code subject`() {
            assertEquals(
                "Код подтверждения для восстановления доступа к TestSys",
                l.user.accessRestorationConfirmationCodeSubject(),
            )
        }

        @Test
        fun `should greet without nickname and show the code on its own line in access restoration confirmation code body`() {
            assertEquals(
                "Здравствуйте!\n\n" +
                    "Вы запросили восстановление доступа к TestSys. Чтобы получить новый Код-доступа, введите этот " +
                    "код на вкладке «Восстановление доступа» страницы входа:\n\n" +
                    "01234567\n\n" +
                    "Никому не сообщайте этот код.\n\n" +
                    "Код действует ограниченное время. Если код больше не действует, запросите восстановление " +
                    "доступа заново, и Вам придёт новый код.\n\n" +
                    "Если Вам пришло несколько таких писем, введите код из последнего.\n\n" +
                    "Если Вы не запрашивали восстановление доступа к TestSys, ничего делать не нужно. " +
                    "Просто удалите это письмо.",
                l.user.accessRestorationConfirmationCodeBody(confirmationCode = "01234567"),
            )
        }
    }

    @Nested
    inner class UiPageTests {

        @Test
        fun `should name the product in brand name`() {
            assertEquals("TestSys", l.uiPage.brandName())
        }

        @Test
        fun `should label the sign in link in header sign in`() {
            assertEquals("Войти", l.uiPage.headerSignIn())
        }

        @Test
        fun `should name the section search in header search`() {
            assertEquals("Поиск по разделам", l.uiPage.headerSearch())
        }

        @Test
        fun `should report no matches in header search empty`() {
            assertEquals("Ничего не найдено", l.uiPage.headerSearchEmpty())
        }

        @Test
        fun `should head the popup in header notifications`() {
            assertEquals("Уведомления", l.uiPage.headerNotifications())
        }

        @Test
        fun `should report no notifications in header notifications empty`() {
            assertEquals("Нет уведомлений", l.uiPage.headerNotificationsEmpty())
        }

        @Test
        fun `should label the read all action in header read all`() {
            assertEquals("Прочитать все", l.uiPage.headerReadAll())
        }

        @Test
        fun `should group digits of the count in header unread count`() {
            assertEquals("Уведомления: непрочитанных 1${NBSP}234", l.uiPage.headerUnreadCount(count = 1234))
        }

        @Test
        fun `should mark the notification as unread in header unread item`() {
            assertEquals("Новое решение — не прочитано", l.uiPage.headerUnreadItem(label = "Новое решение"))
        }

        @Test
        fun `should name the user in header user menu`() {
            assertEquals("Меню пользователя: Маша", l.uiPage.headerUserMenu(name = "Маша"))
        }

        @Test
        fun `should label the open action in header arrival open`() {
            assertEquals("Открыть", l.uiPage.headerArrivalOpen())
        }

        @Test
        fun `should label the close action in header arrival close`() {
            assertEquals("Закрыть уведомление", l.uiPage.headerArrivalClose())
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
        fun `should agree the phrase with the count in header arrival count`(count: Int, expected: String) {
            assertEquals(expected, l.uiPage.headerArrivalCount(count = count))
        }

        @Test
        fun `should group digits of a large count in header arrival count`() {
            assertEquals("Получено 1${NBSP}001 новое уведомление", l.uiPage.headerArrivalCount(count = 1001))
        }

        @Test
        fun `should point to the notification list in header arrival batch hint`() {
            assertEquals(
                "Откройте список уведомлений, чтобы посмотреть новые сообщения.",
                l.uiPage.headerArrivalBatchHint(),
            )
        }

        @Test
        fun `should name the breadcrumbs in navigation breadcrumbs`() {
            assertEquals("Навигационная цепочка", l.uiPage.navigationBreadcrumbs())
        }

        @Test
        fun `should name the sections navigation in navigation sections`() {
            assertEquals("Разделы", l.uiPage.navigationSections())
        }

        @Test
        fun `should show the year without digit grouping in footer year`() {
            assertEquals("2026", l.uiPage.footerYear(year = 2026))
        }

        @Test
        fun `should name the footer navigation in footer links`() {
            assertEquals("Ссылки футера", l.uiPage.footerLinks())
        }

        @Test
        fun `should name the page guide button in guide page`() {
            assertEquals("Открыть гайд страницы", l.uiPage.guidePage())
        }

        @Test
        fun `should name the block explanation button in guide block`() {
            assertEquals("Показать пояснение блока", l.uiPage.guideBlock())
        }

        @Test
        fun `should name the previous step button in guide previous`() {
            assertEquals("Предыдущий шаг", l.uiPage.guidePrevious())
        }

        @Test
        fun `should name the next step button in guide next`() {
            assertEquals("Следующий шаг", l.uiPage.guideNext())
        }

        @Test
        fun `should name the last step button in guide done`() {
            assertEquals("Завершить гайд", l.uiPage.guideDone())
        }

        @Test
        fun `should name the close button in guide close`() {
            assertEquals("Закрыть гайд", l.uiPage.guideClose())
        }
    }

    @Nested
    inner class UiFormTests {

        @Test
        fun `should ask to check the format in field bad input`() {
            assertEquals("Проверьте формат значения", l.uiForm.fieldBadInput())
        }

        @Test
        fun `should report a too small value in field below min`() {
            assertEquals("Значение меньше допустимого", l.uiForm.fieldBelowMin())
        }

        @Test
        fun `should report a too large value in field above max`() {
            assertEquals("Значение больше допустимого", l.uiForm.fieldAboveMax())
        }

        @Test
        fun `should report a value off the step in field step mismatch`() {
            assertEquals("Значение не соответствует шагу", l.uiForm.fieldStepMismatch())
        }

        @Test
        fun `should name the date part in date field date`() {
            assertEquals("дата", l.uiForm.dateFieldDate())
        }

        @Test
        fun `should name the time part in date field time`() {
            assertEquals("время", l.uiForm.dateFieldTime())
        }

        @Test
        fun `should name the start picker after the field in date field range from`() {
            assertEquals("Период: с", l.uiForm.dateFieldRangeFrom(label = "Период"))
        }

        @Test
        fun `should name the end picker after the field in date field range to`() {
            assertEquals("Период: до", l.uiForm.dateFieldRangeTo(label = "Период"))
        }

        @Test
        fun `should show the start prefix in date field range from prefix`() {
            assertEquals("С", l.uiForm.dateFieldRangeFromPrefix())
        }

        @Test
        fun `should show the end prefix in date field range to prefix`() {
            assertEquals("До", l.uiForm.dateFieldRangeToPrefix())
        }

        @Test
        fun `should ask for at least one bound in date field range required`() {
            assertEquals("Укажите хотя бы одну границу периода", l.uiForm.dateFieldRangeRequired())
        }

        @Test
        fun `should report a reversed range in date field range reversed`() {
            assertEquals("Дата окончания раньше даты начала", l.uiForm.dateFieldRangeReversed())
        }

        @Test
        fun `should label the today button in calendar today`() {
            assertEquals("Сегодня", l.uiForm.calendarToday())
        }

        @Test
        fun `should label the cancel button in calendar cancel`() {
            assertEquals("Отменить", l.uiForm.calendarCancel())
        }

        @Test
        fun `should name the previous month button in calendar previous month`() {
            assertEquals("Показать предыдущий месяц", l.uiForm.calendarPreviousMonth())
        }

        @Test
        fun `should name the next month button in calendar next month`() {
            assertEquals("Показать следующий месяц", l.uiForm.calendarNextMonth())
        }

        @Test
        fun `should name the field after the colon in calendar open`() {
            assertEquals("Открыть календарь: Начало тура", l.uiForm.calendarOpen(label = "Начало тура"))
        }

        @Test
        fun `should show the search placeholder in lookup search`() {
            assertEquals("Поиск", l.uiForm.lookupSearch())
        }

        @Test
        fun `should label the open action in lookup open`() {
            assertEquals("Выбрать", l.uiForm.lookupOpen())
        }

        @Test
        fun `should label the clear action in lookup clear`() {
            assertEquals("Очистить", l.uiForm.lookupClear())
        }

        @Test
        fun `should report no matches in lookup empty`() {
            assertEquals("Ничего не найдено", l.uiForm.lookupEmpty())
        }

        @Test
        fun `should quote the removed value in guillemets in lookup remove`() {
            assertEquals("Убрать «Маша»", l.uiForm.lookupRemove(value = "Маша"))
        }

        @Test
        fun `should label the reset action in lookup reset`() {
            assertEquals("Сбросить", l.uiForm.lookupReset())
        }

        @Test
        fun `should label the apply action in lookup apply`() {
            assertEquals("Применить", l.uiForm.lookupApply())
        }

        @Test
        fun `should group digits of the count in lookup selected count`() {
            assertEquals("Выбрано: 1${NBSP}500", l.uiForm.lookupSelectedCount(count = 1500))
        }

        @Test
        fun `should label the select all checkbox in lookup select all`() {
            assertEquals("Выбрать все", l.uiForm.lookupSelectAll())
        }

        @Test
        fun `should group digits of the hidden count in lookup more`() {
            assertEquals("Ещё 1${NBSP}200", l.uiForm.lookupMore(count = 1200))
        }

        @Test
        fun `should prefix the hidden count with a plus in lookup more chip`() {
            assertEquals("+2", l.uiForm.lookupMoreChip(count = 2))
        }

        @Test
        fun `should group digits of a large hidden count in lookup more chip`() {
            assertEquals("+1${NBSP}200", l.uiForm.lookupMoreChip(count = 1200))
        }

        @Test
        fun `should label the edit action in editing start`() {
            assertEquals("Изменить", l.uiForm.editingStart())
        }

        @Test
        fun `should label the save action in editing save`() {
            assertEquals("Сохранить", l.uiForm.editingSave())
        }

        @Test
        fun `should label the cancel action in editing cancel`() {
            assertEquals("Отменить", l.uiForm.editingCancel())
        }
    }

    @Nested
    inner class UiDataTests {

        @Test
        fun `should report no rows in table empty`() {
            assertEquals("Нет данных", l.uiData.tableEmpty())
        }

        @Test
        fun `should join the bounds with an en dash in table range`() {
            assertEquals("1–20 из 1${NBSP}412", l.uiData.tableRange(from = 1, to = 20, total = 1412))
        }

        @Test
        fun `should group digits of every number in table range`() {
            assertEquals(
                "1${NBSP}001–1${NBSP}020 из 12${NBSP}345",
                l.uiData.tableRange(from = 1001, to = 1020, total = 12_345),
            )
        }

        @Test
        fun `should name the header checkbox in table select all`() {
            assertEquals("Выбрать все строки страницы", l.uiData.tableSelectAll())
        }

        @Test
        fun `should name the row checkbox in table select row`() {
            assertEquals("Выбрать строку", l.uiData.tableSelectRow())
        }

        @Test
        fun `should label the filter panel in table filters title`() {
            assertEquals("Настроить фильтры", l.uiData.tableFiltersTitle())
        }

        @Test
        fun `should label the apply button in table filters apply`() {
            assertEquals("Применить", l.uiData.tableFiltersApply())
        }

        @Test
        fun `should label the reset button in table filters reset`() {
            assertEquals("Сбросить", l.uiData.tableFiltersReset())
        }

        @Test
        fun `should name the previous page button in pagination previous`() {
            assertEquals("Перейти на предыдущую страницу", l.uiData.paginationPrevious())
        }

        @Test
        fun `should name the next page button in pagination next`() {
            assertEquals("Перейти на следующую страницу", l.uiData.paginationNext())
        }

        @Test
        fun `should name the page button by number in pagination page`() {
            assertEquals("Страница 7", l.uiData.paginationPage(page = 7))
        }

        @Test
        fun `should group digits of a large number in pagination page`() {
            assertEquals("Страница 1${NBSP}000", l.uiData.paginationPage(page = 1000))
        }

        @Test
        fun `should separate the page and the count with a spaced slash in pagination counter`() {
            assertEquals("3 / 71", l.uiData.paginationCounter(page = 3, count = 71))
        }

        @Test
        fun `should group digits of a large count in pagination counter`() {
            assertEquals("1 / 1${NBSP}412", l.uiData.paginationCounter(page = 1, count = 1412))
        }
    }

    @Nested
    inner class UiOverlayTests {

        @Test
        fun `should label the cancel action in dialog cancel`() {
            assertEquals("Отменить", l.uiOverlay.dialogCancel())
        }

        @Test
        fun `should label the close button in dialog close`() {
            assertEquals("Закрыть", l.uiOverlay.dialogClose())
        }

        @Test
        fun `should quote the name in guillemets in dialog type to confirm`() {
            assertEquals(
                "Введите «Весенний тур», чтобы подтвердить",
                l.uiOverlay.dialogTypeToConfirm(name = "Весенний тур"),
            )
        }

        @Test
        fun `should name the action menu button in menu actions`() {
            assertEquals("Действия", l.uiOverlay.menuActions())
        }
    }

    @Nested
    inner class UiFeedbackTests {

        @Test
        fun `should report the failure in load failed`() {
            assertEquals("Не удалось загрузить", l.uiFeedback.loadFailed())
        }

        @Test
        fun `should suggest another attempt in load failed hint`() {
            assertEquals("Попробуйте ещё раз", l.uiFeedback.loadFailedHint())
        }

        @Test
        fun `should label the retry action in load retry`() {
            assertEquals("Повторить", l.uiFeedback.loadRetry())
        }

        @Test
        fun `should name the skeleton in load loading`() {
            assertEquals("Загрузка", l.uiFeedback.loadLoading())
        }

        @Test
        fun `should label the back action in error back`() {
            assertEquals("Вернуться", l.uiFeedback.errorBack())
        }

        @Test
        fun `should state the missing page in error not found title`() {
            assertEquals("Страница не найдена", l.uiFeedback.errorNotFoundTitle())
        }

        @Test
        fun `should suggest checking the address in error not found description`() {
            assertEquals("Проверьте адрес или вернитесь на предыдущую страницу.", l.uiFeedback.errorNotFoundDescription())
        }

        @Test
        fun `should append the product name in error not found page title`() {
            assertEquals("Страница не найдена — TestSys", l.uiFeedback.errorNotFoundPageTitle())
        }

        @Test
        fun `should state the denied access in error forbidden title`() {
            assertEquals("Нет доступа", l.uiFeedback.errorForbiddenTitle())
        }

        @Test
        fun `should explain the denied access in error forbidden description`() {
            assertEquals(
                "У вас нет доступа к этой странице или нет нужной для неё роли.",
                l.uiFeedback.errorForbiddenDescription(),
            )
        }

        @Test
        fun `should append the product name in error forbidden page title`() {
            assertEquals("Нет доступа — TestSys", l.uiFeedback.errorForbiddenPageTitle())
        }

        @Test
        fun `should state the failure in error page failed title`() {
            assertEquals("Не удалось открыть страницу", l.uiFeedback.errorPageFailedTitle())
        }

        @Test
        fun `should suggest reloading in error page failed description`() {
            assertEquals("Обновите страницу или вернитесь на предыдущую.", l.uiFeedback.errorPageFailedDescription())
        }

        @Test
        fun `should append the product name in error page failed page title`() {
            assertEquals("Не удалось открыть страницу — TestSys", l.uiFeedback.errorPageFailedPageTitle())
        }

        @Test
        fun `should report a missing object in error failure not found`() {
            assertEquals("Объект не найден", l.uiFeedback.errorFailureNotFound())
        }

        @Test
        fun `should report the denied access in error failure access denied`() {
            assertEquals("Нет доступа", l.uiFeedback.errorFailureAccessDenied())
        }

        @Test
        fun `should report the missing role in error failure missing role`() {
            assertEquals("Нет нужной роли", l.uiFeedback.errorFailureMissingRole())
        }

        @Test
        fun `should report a resource outside the task in failure resource not in error task`() {
            assertEquals("Ресурс недоступен в этой задаче", l.uiFeedback.errorFailureResourceNotInTask())
        }

        @Test
        fun `should report any other failure in error failure failed`() {
            assertEquals("Действие не выполнено", l.uiFeedback.errorFailureFailed())
        }

        @Test
        fun `should label the days tile in timer days`() {
            assertEquals("дни", l.uiFeedback.timerDays())
        }

        @Test
        fun `should label the hours tile in timer hours`() {
            assertEquals("часы", l.uiFeedback.timerHours())
        }

        @Test
        fun `should label the minutes tile in timer minutes`() {
            assertEquals("минуты", l.uiFeedback.timerMinutes())
        }

        @Test
        fun `should label the seconds tile in timer seconds`() {
            assertEquals("секунды", l.uiFeedback.timerSeconds())
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
                "Если вы потеряли Код-доступа, получите новый: укажите привязанную почту и введите код из письма.",
                l.pageGuest.restoreAccessGuide(),
            )
        }

        @Test
        fun `should explain the email step in restore access block guide`() {
            assertEquals(
                "Укажите привязанную почту и нажмите «Получить код»: на неё придёт письмо с кодом.",
                l.pageGuest.restoreAccessBlockGuide(),
            )
        }

        @Test
        fun `should head the email step guide in restore access block guide title`() {
            assertEquals("Запрос кода", l.pageGuest.restoreAccessBlockGuideTitle())
        }

        @Test
        fun `should label the email field in restore access email`() {
            assertEquals("Почта", l.pageGuest.restoreAccessEmail())
        }

        @Test
        fun `should ask for the bound email in restore access email guide`() {
            assertEquals("Введите почту, привязанную к вашему Профилю.", l.pageGuest.restoreAccessEmailGuide())
        }

        @Test
        fun `should report the accepted request in restore access requested title`() {
            assertEquals("Запрос принят", l.pageGuest.restoreAccessRequestedTitle())
        }

        @Test
        fun `should promise the letter only for a bound email in restore access requested description`() {
            assertEquals(
                "Если почта masha@example.com привязана к Пользователю, на неё придёт письмо с кодом.",
                l.pageGuest.restoreAccessRequestedDescription(email = "masha@example.com"),
            )
        }

        @Test
        fun `should point to the last letter in restore access code guide`() {
            assertEquals(
                "Введите код из последнего письма о восстановлении доступа.",
                l.pageGuest.restoreAccessCodeGuide(),
            )
        }

        @Test
        fun `should explain the code step in restore access code step block guide`() {
            assertEquals(
                "Введите код из письма и нажмите «Восстановить доступ».",
                l.pageGuest.restoreAccessCodeStepBlockGuide(),
            )
        }

        @Test
        fun `should head the code step guide in restore access code step block guide title`() {
            assertEquals("Ввод кода", l.pageGuest.restoreAccessCodeStepBlockGuideTitle())
        }

        @Test
        fun `should label the main action in restore access button`() {
            assertEquals("Восстановить доступ", l.pageGuest.restoreAccessButton())
        }

        @Test
        fun `should report a missing request in restore access request not found`() {
            assertEquals("Запрос восстановления не найден", l.pageGuest.restoreAccessRequestNotFound())
        }

        @Test
        fun `should report the restored access in restore access completed title`() {
            assertEquals("Доступ восстановлен", l.pageGuest.restoreAccessCompletedTitle())
        }

        @Test
        fun `should render the access code term in dative in restore access completed description`() {
            assertEquals("Войдите по новому Коду-доступа.", l.pageGuest.restoreAccessCompletedDescription())
        }

        @Test
        fun `should render the access code term in nominative in restore access result title`() {
            assertEquals("Новый Код-доступа", l.pageGuest.restoreAccessResultTitle())
        }

        @Test
        fun `should render the access code term in accusative in restore access result block guide`() {
            assertEquals(
                "Сохраните новый Код-доступа и войдите по нему на вкладке «Вход». При повторном открытии страницы " +
                    "Код-доступа больше не будет показан.",
                l.pageGuest.restoreAccessResultBlockGuide(),
            )
        }

        @Test
        fun `should render the access code term in accusative in restore access result alert title`() {
            assertEquals("Сохраните Код-доступа", l.pageGuest.restoreAccessResultAlertTitle())
        }

        @Test
        fun `should warn about the old code and the mail in restore access result alert text`() {
            assertEquals(
                "По нему вы входите в TestSys. Прежний Код-доступа больше не действует. Новый код на почту " +
                    "не отправляется.",
                l.pageGuest.restoreAccessResultAlertText(),
            )
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
        fun `should name the profile of the user without the e-mail change in impersonation guide`() {
            assertEquals("Здесь можно просмотреть Профиль Пользователя.", l.pageProfile.impersonationGuide())
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

        @Test
        fun `should mark the supervisor sign in after a middle dot joined by no-break spaces in impersonation name`() {
            assertEquals("Маша$NBSP·${NBSP}вход${NBSP}Супервайзера", l.pageCabinet.impersonationName(name = "Маша"))
        }

        @Test
        fun `should use the dative supervisor term in impersonation return`() {
            assertEquals("Вернуться к Супервайзеру", l.pageCabinet.impersonationReturn())
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

        @ParameterizedTest
        @CsvSource(
            "1,   Не длиннее 1 символа",
            "21,  Не длиннее 21 символа",
            "3,   Не длиннее 3 символов",
            "255, Не длиннее 255 символов",
        )
        fun `should decline the character count by plural category in name too long`(max: Int, expected: String) {
            assertEquals(expected, l.pageCabinet.nameTooLong(max = max))
        }

        @ParameterizedTest
        @CsvSource(
            "NOT_STARTED, Не запускалась",
            "RUNNING,     Идёт",
            "SUCCESS,     Успех",
            "FAILURE,     Неуспех",
            "STOPPED,     Техническая остановка",
            "OTHER,       Неизвестный статус",
        )
        fun `should name the task validation status in validation status`(status: ValidationStatusStatus, expected: String) {
            assertEquals(expected, l.pageCabinet.validationStatus(status = status))
        }

        @Test
        fun `should say there is no limit in duration unlimited`() {
            assertEquals("Без ограничения", l.pageCabinet.durationUnlimited())
        }

        @ParameterizedTest
        @CsvSource(
            "1,                   1 ч",
            "1000,                1000 ч",
            "2562047788015215,    2562047788015215 ч",
        )
        fun `should show hours without digit grouping in duration hours`(hours: Long, expected: String) {
            assertEquals(expected, l.pageCabinet.durationHours(hours = hours))
        }

        @ParameterizedTest
        @CsvSource(
            "0,  0 мин",
            "1,  1 мин",
            "59, 59 мин",
        )
        fun `should show minutes in duration minutes`(minutes: Int, expected: String) {
            assertEquals(expected, l.pageCabinet.durationMinutes(minutes = minutes))
        }

        @ParameterizedTest
        @CsvSource(
            "1,    30, 1 ч 30 мин",
            "1000, 59, 1000 ч 59 мин",
        )
        fun `should show hours without digit grouping and minutes in duration hours minutes`(hours: Long, minutes: Int, expected: String) {
            assertEquals(expected, l.pageCabinet.durationHoursMinutes(hours = hours, minutes = minutes))
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
    inner class PageStudentTests {

        @Test
        fun `should name the student cabinet in title`() {
            assertEquals("Кабинет Ученика", l.pageStudent.title())
        }

        @Test
        fun `should decline the class and invite code terms in guide`() {
            assertEquals(
                "Откройте свой Класс или присоединитесь к новому по Коду-приглашению.",
                l.pageStudent.guide(),
            )
        }

        @Test
        fun `should use the genitive plural class term in classes guide`() {
            assertEquals("Список Классов и сведения о них.", l.pageStudent.classesGuide())
        }

        @Test
        fun `should use the genitive plural class term in classes empty`() {
            assertEquals("Классов пока нет", l.pageStudent.classesEmpty())
        }

        @Test
        fun `should use the dative class and invite code terms in classes empty hint`() {
            assertEquals("Присоединитесь к Классу по Коду-приглашению.", l.pageStudent.classesEmptyHint())
        }

        @Test
        fun `should use the dative class term in join title`() {
            assertEquals("Присоединиться к Классу", l.pageStudent.joinTitle())
        }

        @Test
        fun `should quote the join button in join guide`() {
            assertEquals(
                "Введите Код-приглашение от Организатора и нажмите «Присоединиться».",
                l.pageStudent.joinGuide(),
            )
        }

        @Test
        fun `should use the invite code term in join invite code`() {
            assertEquals("Код-приглашение", l.pageStudent.joinInviteCode())
        }

        @Test
        fun `should name the joining action in join submit`() {
            assertEquals("Присоединиться", l.pageStudent.joinSubmit())
        }

        @Test
        fun `should agree with the masculine invite code term in join invite code invalid`() {
            assertEquals("Код-приглашение недействителен", l.pageStudent.joinInviteCodeInvalid())
        }

        @Test
        fun `should use the genitive invite code term in join invite code expired`() {
            assertEquals("Срок Кода-приглашения истёк", l.pageStudent.joinInviteCodeExpired())
        }

        @Test
        fun `should quote the class name in join joined`() {
            assertEquals("Вы присоединились к Классу «7А»", l.pageStudent.joinJoined(name = "7А"))
        }

        @Test
        fun `should use the class term in class tab title`() {
            assertEquals("Класс", l.pageStudent.classTabTitle())
        }

        @Test
        fun `should quote the class name in class title`() {
            assertEquals("Класс «7А»", l.pageStudent.classTitle(name = "7А"))
        }

        @Test
        fun `should decline the class and contest terms in class guide`() {
            assertEquals("На странице Класса показаны его сведения и доступные Туры.", l.pageStudent.classGuide())
        }

        @Test
        fun `should use the genitive class term in class details guide`() {
            assertEquals(
                "Идентификатор, название и описание Класса, к которому вы присоединились.",
                l.pageStudent.classDetailsGuide(),
            )
        }

        @Test
        fun `should decline the contest and class terms in class contests empty hint`() {
            assertEquals(
                "Туры появятся, когда Организатор добавит их в Класс.",
                l.pageStudent.classContestsEmptyHint(),
            )
        }

        @Test
        fun `should decline the contest term in contests guide`() {
            assertEquals(
                "Список Туров и их параметры. Выберите строку, чтобы открыть Тур.",
                l.pageStudent.contestsGuide(),
            )
        }

        @Test
        fun `should use the genitive plural contest term in contests empty`() {
            assertEquals("Туров пока нет", l.pageStudent.contestsEmpty())
        }

        @Test
        fun `should name the id in id`() {
            assertEquals("ID", l.pageStudent.id())
        }

        @Test
        fun `should name the name in name`() {
            assertEquals("Название", l.pageStudent.name())
        }

        @Test
        fun `should name the description in description`() {
            assertEquals("Описание", l.pageStudent.description())
        }

        @Test
        fun `should head the details block in details title`() {
            assertEquals("Сведения", l.pageStudent.detailsTitle())
        }

        @Test
        fun `should name the start in starts at`() {
            assertEquals("Начало", l.pageStudent.startsAt())
        }

        @Test
        fun `should name the end in ends at`() {
            assertEquals("Конец", l.pageStudent.endsAt())
        }

        @Test
        fun `should name the time allowed in attempt duration`() {
            assertEquals("Время на прохождение", l.pageStudent.attemptDuration())
        }

        @Test
        fun `should name the state column in contest state title`() {
            assertEquals("Состояние", l.pageStudent.contestStateTitle())
        }

        @ParameterizedTest
        @CsvSource(
            "NOT_STARTED, Не начат",
            "RUNNING,     Идёт",
            "FINISHED,    Завершён",
            "OTHER,       Неизвестное состояние",
        )
        fun `should name the contest state in contest state`(state: PageStudent.ContestStateState, expected: String) {
            assertEquals(expected, l.pageStudent.contestState(state = state))
        }

        @Test
        fun `should name the remaining time in remaining title`() {
            assertEquals("Осталось", l.pageStudent.remainingTitle())
        }

        @Test
        fun `should decline the contest and solution terms in remaining guide`() {
            assertEquals(
                "Время, оставшееся для работы в Туре. После его окончания отправка Решений недоступна.",
                l.pageStudent.remainingGuide(),
            )
        }

        @Test
        fun `should say the time is over in remaining over`() {
            assertEquals("Время вышло", l.pageStudent.remainingOver())
        }

        @Test
        fun `should use the genitive contest term in remaining not started`() {
            assertEquals("Отсчёт начнётся с началом Тура", l.pageStudent.remainingNotStarted())
        }

        @Test
        fun `should say there is no time limit in remaining unlimited`() {
            assertEquals("Без ограничения по времени", l.pageStudent.remainingUnlimited())
        }

        @ParameterizedTest
        @CsvSource(
            "QUEUED,        В очереди",
            "IN_PROGRESS,   Проверяется",
            "SUCCESS,       Проверено",
            "GRADING_ERROR, Ошибка проверки",
            "TIMEOUT,       Превышено время проверки",
            "OTHER,         Неизвестный статус",
        )
        fun `should name the grading status in submission status`(status: PageStudent.SubmissionStatusStatus, expected: String) {
            assertEquals(expected, l.pageStudent.submissionStatus(status = status))
        }

        @Test
        fun `should use the contest term in contest tab title`() {
            assertEquals("Тур", l.pageStudent.contestTabTitle())
        }

        @Test
        fun `should quote the contest name in contest title`() {
            assertEquals("Тур «Весенний»", l.pageStudent.contestTitle(name = "Весенний"))
        }

        @Test
        fun `should keep the contest conditions lowercase in contest guide`() {
            assertEquals(
                "Здесь показаны условия Тура, оставшееся время и доступные Задачи.",
                l.pageStudent.contestGuide(),
            )
        }

        @Test
        fun `should use the accusative contest term in contest start`() {
            assertEquals("Начать Тур", l.pageStudent.contestStart())
        }

        @Test
        fun `should quote the contest name in contest start confirm title`() {
            assertEquals("Начать Тур «Весенний»?", l.pageStudent.contestStartConfirmTitle(name = "Весенний"))
        }

        @Test
        fun `should warn that the time starts in contest start confirm text`() {
            assertEquals(
                "С этого момента отсчитывается время на прохождение.",
                l.pageStudent.contestStartConfirmText(),
            )
        }

        @Test
        fun `should name the starting action in contest start confirm action`() {
            assertEquals("Начать", l.pageStudent.contestStartConfirmAction())
        }

        @Test
        fun `should use the contest term in contest started`() {
            assertEquals("Тур начат", l.pageStudent.contestStarted())
        }

        @Test
        fun `should use the genitive contest term in contest details guide`() {
            assertEquals(
                "Название и описание Тура, время начала и окончания и ограничение времени на прохождение.",
                l.pageStudent.contestDetailsGuide(),
            )
        }

        @Test
        fun `should use the genitive plural task term in contest tasks guide`() {
            assertEquals("Список Задач и их основные сведения.", l.pageStudent.contestTasksGuide())
        }

        @Test
        fun `should name the best score in contest tasks score`() {
            assertEquals("Балл", l.pageStudent.contestTasksScore())
        }

        @Test
        fun `should use the submission term in contest tasks last submission`() {
            assertEquals("Последняя Посылка", l.pageStudent.contestTasksLastSubmission())
        }

        @Test
        fun `should use the genitive plural task term in contest tasks empty`() {
            assertEquals("Задач пока нет", l.pageStudent.contestTasksEmpty())
        }

        @Test
        fun `should use the task term in task tab title`() {
            assertEquals("Задача", l.pageStudent.taskTabTitle())
        }

        @Test
        fun `should quote the task name in task title`() {
            assertEquals("Задача «Лабиринт»", l.pageStudent.taskTitle(name = "Лабиринт"))
        }

        @Test
        fun `should decline the task solution and submission terms in task guide`() {
            assertEquals(
                "Ознакомьтесь с Задачей, отправьте Решение и просмотрите результаты Посылок.",
                l.pageStudent.taskGuide(),
            )
        }

        @Test
        fun `should quote the files field in task details guide`() {
            assertEquals(
                "Название и описание Задачи. В строке «Файлы» можно скачать доступные Ресурсы.",
                l.pageStudent.taskDetailsGuide(),
            )
        }

        @Test
        fun `should name the files in task files`() {
            assertEquals("Файлы", l.pageStudent.taskFiles())
        }

        @Test
        fun `should say there are no files in task files empty`() {
            assertEquals("Нет файлов", l.pageStudent.taskFilesEmpty())
        }

        @Test
        fun `should name the statement in task download statement`() {
            assertEquals("Скачать Условие", l.pageStudent.taskDownloadStatement())
        }

        @Test
        fun `should quote the exercise name in task download exercise`() {
            assertEquals("Скачать Упражнение «main.qrs»", l.pageStudent.taskDownloadExercise(name = "main.qrs"))
        }

        @Test
        fun `should name the best result in task best`() {
            assertEquals("Лучший результат", l.pageStudent.taskBest())
        }

        @Test
        fun `should say there is no best result in task best none`() {
            assertEquals("Нет", l.pageStudent.taskBestNone())
        }

        @Test
        fun `should use the genitive solution term in task send title`() {
            assertEquals("Отправка Решения", l.pageStudent.taskSendTitle())
        }

        @Test
        fun `should use the genitive task term in task send unavailable`() {
            assertEquals(
                "Отправка недоступна: у Задачи нет разрешённых языков",
                l.pageStudent.taskSendUnavailable(),
            )
        }

        @Test
        fun `should name the language in task send language`() {
            assertEquals("Язык", l.pageStudent.taskSendLanguage())
        }

        @ParameterizedTest
        @CsvSource(
            "PYTHON,          Python",
            "JAVASCRIPT,      JavaScript",
            "VISUAL_LANGUAGE, Визуальный язык TRIK Studio",
            "OTHER,           Неизвестный язык",
        )
        fun `should name the language in task send language name`(language: PageStudent.TaskSendLanguageNameLanguage, expected: String) {
            assertEquals(expected, l.pageStudent.taskSendLanguageName(language = language))
        }

        @Test
        fun `should use the genitive solution term in task send file`() {
            assertEquals("Файл Решения", l.pageStudent.taskSendFile())
        }

        @Test
        fun `should decline the solution and contest terms in task send guide`() {
            assertEquals(
                "Выберите язык, добавьте файл Решения и отправьте его на проверку, пока Тур доступен.",
                l.pageStudent.taskSendGuide(),
            )
        }

        @Test
        fun `should use the genitive solution term in task send language step title`() {
            assertEquals("Язык Решения", l.pageStudent.taskSendLanguageStepTitle())
        }

        @Test
        fun `should ask for the file language in task send language step description`() {
            assertEquals(
                "Выберите язык, на котором написан отправляемый файл.",
                l.pageStudent.taskSendLanguageStepDescription(),
            )
        }

        @Test
        fun `should decline the solution and task terms in task send file step description`() {
            assertEquals(
                "Прикрепите один файл с Решением выбранной Задачи.",
                l.pageStudent.taskSendFileStepDescription(),
            )
        }

        @Test
        fun `should name the sending action in task send submit`() {
            assertEquals("Отправить", l.pageStudent.taskSendSubmit())
        }

        @Test
        fun `should ask for the language in task send language required`() {
            assertEquals("Выберите язык", l.pageStudent.taskSendLanguageRequired())
        }

        @Test
        fun `should ask for the file in task send file required`() {
            assertEquals("Выберите файл", l.pageStudent.taskSendFileRequired())
        }

        @Test
        fun `should agree with the neuter solution term in task send sent`() {
            assertEquals("Решение отправлено", l.pageStudent.taskSendSent())
        }

        @Test
        fun `should use the plural solution term in task submissions title`() {
            assertEquals("Решения", l.pageStudent.taskSubmissionsTitle())
        }

        @Test
        fun `should decline the submission and task terms in task submissions guide`() {
            assertEquals(
                "История ваших Посылок по этой Задаче, их состояние и результаты проверки.",
                l.pageStudent.taskSubmissionsGuide(),
            )
        }

        @Test
        fun `should name the sending time in task submissions sent`() {
            assertEquals("Отправлено", l.pageStudent.taskSubmissionsSent())
        }

        @Test
        fun `should name the file in task submissions file`() {
            assertEquals("Файл", l.pageStudent.taskSubmissionsFile())
        }

        @Test
        fun `should name the result in task submissions result`() {
            assertEquals("Результат", l.pageStudent.taskSubmissionsResult())
        }

        @Test
        fun `should mark the best submission in task submissions best`() {
            assertEquals("Лучшее", l.pageStudent.taskSubmissionsBest())
        }

        @Test
        fun `should name the download action in task submissions download`() {
            assertEquals("Скачать", l.pageStudent.taskSubmissionsDownload())
        }

        @Test
        fun `should use the genitive plural solution term in task submissions empty`() {
            assertEquals("Решений пока нет", l.pageStudent.taskSubmissionsEmpty())
        }

        @Test
        fun `should use the accusative solution term in task submissions empty hint sendable`() {
            assertEquals("Отправьте Решение в форме выше.", l.pageStudent.taskSubmissionsEmptyHintSendable())
        }

        @Test
        fun `should decline the submission and contest terms in task submissions empty hint closed`() {
            assertEquals("Посылок в этом Туре нет.", l.pageStudent.taskSubmissionsEmptyHintClosed())
        }
    }

    @Nested
    inner class PageParticipantTests {

        @Test
        fun `should name the participant cabinet in title`() {
            assertEquals("Кабинет Участника", l.pageParticipant.title())
        }

        @Test
        fun `should decline the contest and task terms in guide`() {
            assertEquals("Выберите назначенный Тур, чтобы перейти к его Задачам.", l.pageParticipant.guide())
        }

        @Test
        fun `should use the plural contest term in contests empty hint`() {
            assertEquals(
                "Туры появятся, когда Организатор добавит их в Соревнование.",
                l.pageParticipant.contestsEmptyHint(),
            )
        }

        @Test
        fun `should decline the contest term in contests guide`() {
            assertEquals(
                "Список Туров и их параметры. Выберите строку, чтобы открыть Тур.",
                l.pageParticipant.contestsGuide(),
            )
        }

        @Test
        fun `should use the genitive plural contest term in contests empty`() {
            assertEquals("Туров пока нет", l.pageParticipant.contestsEmpty())
        }

        @Test
        fun `should name the id in id`() {
            assertEquals("ID", l.pageParticipant.id())
        }

        @Test
        fun `should name the name in name`() {
            assertEquals("Название", l.pageParticipant.name())
        }

        @Test
        fun `should name the description in description`() {
            assertEquals("Описание", l.pageParticipant.description())
        }

        @Test
        fun `should head the details block in details title`() {
            assertEquals("Сведения", l.pageParticipant.detailsTitle())
        }

        @Test
        fun `should name the start in starts at`() {
            assertEquals("Начало", l.pageParticipant.startsAt())
        }

        @Test
        fun `should name the end in ends at`() {
            assertEquals("Конец", l.pageParticipant.endsAt())
        }

        @Test
        fun `should name the time allowed in attempt duration`() {
            assertEquals("Время на прохождение", l.pageParticipant.attemptDuration())
        }

        @Test
        fun `should name the state column in contest state title`() {
            assertEquals("Состояние", l.pageParticipant.contestStateTitle())
        }

        @ParameterizedTest
        @CsvSource(
            "NOT_STARTED, Не начат",
            "RUNNING,     Идёт",
            "FINISHED,    Завершён",
            "OTHER,       Неизвестное состояние",
        )
        fun `should name the contest state in contest state`(state: PageParticipant.ContestStateState, expected: String) {
            assertEquals(expected, l.pageParticipant.contestState(state = state))
        }

        @Test
        fun `should name the remaining time in remaining title`() {
            assertEquals("Осталось", l.pageParticipant.remainingTitle())
        }

        @Test
        fun `should decline the contest and solution terms in remaining guide`() {
            assertEquals(
                "Время, оставшееся для работы в Туре. После его окончания отправка Решений недоступна.",
                l.pageParticipant.remainingGuide(),
            )
        }

        @Test
        fun `should say the time is over in remaining over`() {
            assertEquals("Время вышло", l.pageParticipant.remainingOver())
        }

        @Test
        fun `should use the genitive contest term in remaining not started`() {
            assertEquals("Отсчёт начнётся с началом Тура", l.pageParticipant.remainingNotStarted())
        }

        @Test
        fun `should say there is no time limit in remaining unlimited`() {
            assertEquals("Без ограничения по времени", l.pageParticipant.remainingUnlimited())
        }

        @ParameterizedTest
        @CsvSource(
            "QUEUED,        В очереди",
            "IN_PROGRESS,   Проверяется",
            "SUCCESS,       Проверено",
            "GRADING_ERROR, Ошибка проверки",
            "TIMEOUT,       Превышено время проверки",
            "OTHER,         Неизвестный статус",
        )
        fun `should name the grading status in submission status`(status: PageParticipant.SubmissionStatusStatus, expected: String) {
            assertEquals(expected, l.pageParticipant.submissionStatus(status = status))
        }

        @Test
        fun `should use the contest term in contest tab title`() {
            assertEquals("Тур", l.pageParticipant.contestTabTitle())
        }

        @Test
        fun `should quote the contest name in contest title`() {
            assertEquals("Тур «Весенний»", l.pageParticipant.contestTitle(name = "Весенний"))
        }

        @Test
        fun `should keep the contest conditions lowercase in contest guide`() {
            assertEquals(
                "Здесь показаны условия Тура, оставшееся время и доступные Задачи.",
                l.pageParticipant.contestGuide(),
            )
        }

        @Test
        fun `should use the accusative contest term in contest start`() {
            assertEquals("Начать Тур", l.pageParticipant.contestStart())
        }

        @Test
        fun `should quote the contest name in contest start confirm title`() {
            assertEquals("Начать Тур «Весенний»?", l.pageParticipant.contestStartConfirmTitle(name = "Весенний"))
        }

        @Test
        fun `should warn that the time starts in contest start confirm text`() {
            assertEquals(
                "С этого момента отсчитывается время на прохождение.",
                l.pageParticipant.contestStartConfirmText(),
            )
        }

        @Test
        fun `should name the starting action in contest start confirm action`() {
            assertEquals("Начать", l.pageParticipant.contestStartConfirmAction())
        }

        @Test
        fun `should use the contest term in contest started`() {
            assertEquals("Тур начат", l.pageParticipant.contestStarted())
        }

        @Test
        fun `should use the genitive contest term in contest details guide`() {
            assertEquals(
                "Название и описание Тура, время начала и окончания и ограничение времени на прохождение.",
                l.pageParticipant.contestDetailsGuide(),
            )
        }

        @Test
        fun `should use the genitive plural task term in contest tasks guide`() {
            assertEquals("Список Задач и их основные сведения.", l.pageParticipant.contestTasksGuide())
        }

        @Test
        fun `should name the best score in contest tasks score`() {
            assertEquals("Балл", l.pageParticipant.contestTasksScore())
        }

        @Test
        fun `should use the submission term in contest tasks last submission`() {
            assertEquals("Последняя Посылка", l.pageParticipant.contestTasksLastSubmission())
        }

        @Test
        fun `should use the genitive plural task term in contest tasks empty`() {
            assertEquals("Задач пока нет", l.pageParticipant.contestTasksEmpty())
        }

        @Test
        fun `should use the task term in task tab title`() {
            assertEquals("Задача", l.pageParticipant.taskTabTitle())
        }

        @Test
        fun `should quote the task name in task title`() {
            assertEquals("Задача «Лабиринт»", l.pageParticipant.taskTitle(name = "Лабиринт"))
        }

        @Test
        fun `should decline the task solution and submission terms in task guide`() {
            assertEquals(
                "Ознакомьтесь с Задачей, отправьте Решение и просмотрите результаты Посылок.",
                l.pageParticipant.taskGuide(),
            )
        }

        @Test
        fun `should quote the files field in task details guide`() {
            assertEquals(
                "Название и описание Задачи. В строке «Файлы» можно скачать доступные Ресурсы.",
                l.pageParticipant.taskDetailsGuide(),
            )
        }

        @Test
        fun `should name the files in task files`() {
            assertEquals("Файлы", l.pageParticipant.taskFiles())
        }

        @Test
        fun `should say there are no files in task files empty`() {
            assertEquals("Нет файлов", l.pageParticipant.taskFilesEmpty())
        }

        @Test
        fun `should name the statement in task download statement`() {
            assertEquals("Скачать Условие", l.pageParticipant.taskDownloadStatement())
        }

        @Test
        fun `should quote the exercise name in task download exercise`() {
            assertEquals("Скачать Упражнение «main.qrs»", l.pageParticipant.taskDownloadExercise(name = "main.qrs"))
        }

        @Test
        fun `should name the best result in task best`() {
            assertEquals("Лучший результат", l.pageParticipant.taskBest())
        }

        @Test
        fun `should say there is no best result in task best none`() {
            assertEquals("Нет", l.pageParticipant.taskBestNone())
        }

        @Test
        fun `should use the genitive solution term in task send title`() {
            assertEquals("Отправка Решения", l.pageParticipant.taskSendTitle())
        }

        @Test
        fun `should use the genitive task term in task send unavailable`() {
            assertEquals(
                "Отправка недоступна: у Задачи нет разрешённых языков",
                l.pageParticipant.taskSendUnavailable(),
            )
        }

        @Test
        fun `should name the language in task send language`() {
            assertEquals("Язык", l.pageParticipant.taskSendLanguage())
        }

        @ParameterizedTest
        @CsvSource(
            "PYTHON,          Python",
            "JAVASCRIPT,      JavaScript",
            "VISUAL_LANGUAGE, Визуальный язык TRIK Studio",
            "OTHER,           Неизвестный язык",
        )
        fun `should name the language in task send language name`(
            language: PageParticipant.TaskSendLanguageNameLanguage,
            expected: String,
        ) {
            assertEquals(expected, l.pageParticipant.taskSendLanguageName(language = language))
        }

        @Test
        fun `should use the genitive solution term in task send file`() {
            assertEquals("Файл Решения", l.pageParticipant.taskSendFile())
        }

        @Test
        fun `should decline the solution and contest terms in task send guide`() {
            assertEquals(
                "Выберите язык, добавьте файл Решения и отправьте его на проверку, пока Тур доступен.",
                l.pageParticipant.taskSendGuide(),
            )
        }

        @Test
        fun `should use the genitive solution term in task send language step title`() {
            assertEquals("Язык Решения", l.pageParticipant.taskSendLanguageStepTitle())
        }

        @Test
        fun `should ask for the file language in task send language step description`() {
            assertEquals(
                "Выберите язык, на котором написан отправляемый файл.",
                l.pageParticipant.taskSendLanguageStepDescription(),
            )
        }

        @Test
        fun `should decline the solution and task terms in task send file step description`() {
            assertEquals(
                "Прикрепите один файл с Решением выбранной Задачи.",
                l.pageParticipant.taskSendFileStepDescription(),
            )
        }

        @Test
        fun `should name the sending action in task send submit`() {
            assertEquals("Отправить", l.pageParticipant.taskSendSubmit())
        }

        @Test
        fun `should ask for the language in task send language required`() {
            assertEquals("Выберите язык", l.pageParticipant.taskSendLanguageRequired())
        }

        @Test
        fun `should ask for the file in task send file required`() {
            assertEquals("Выберите файл", l.pageParticipant.taskSendFileRequired())
        }

        @Test
        fun `should agree with the neuter solution term in task send sent`() {
            assertEquals("Решение отправлено", l.pageParticipant.taskSendSent())
        }

        @Test
        fun `should use the plural solution term in task submissions title`() {
            assertEquals("Решения", l.pageParticipant.taskSubmissionsTitle())
        }

        @Test
        fun `should decline the submission and task terms in task submissions guide`() {
            assertEquals(
                "История ваших Посылок по этой Задаче, их состояние и результаты проверки.",
                l.pageParticipant.taskSubmissionsGuide(),
            )
        }

        @Test
        fun `should name the sending time in task submissions sent`() {
            assertEquals("Отправлено", l.pageParticipant.taskSubmissionsSent())
        }

        @Test
        fun `should name the file in task submissions file`() {
            assertEquals("Файл", l.pageParticipant.taskSubmissionsFile())
        }

        @Test
        fun `should name the result in task submissions result`() {
            assertEquals("Результат", l.pageParticipant.taskSubmissionsResult())
        }

        @Test
        fun `should mark the best submission in task submissions best`() {
            assertEquals("Лучшее", l.pageParticipant.taskSubmissionsBest())
        }

        @Test
        fun `should name the download action in task submissions download`() {
            assertEquals("Скачать", l.pageParticipant.taskSubmissionsDownload())
        }

        @Test
        fun `should use the genitive plural solution term in task submissions empty`() {
            assertEquals("Решений пока нет", l.pageParticipant.taskSubmissionsEmpty())
        }

        @Test
        fun `should use the accusative solution term in task submissions empty hint sendable`() {
            assertEquals("Отправьте Решение в форме выше.", l.pageParticipant.taskSubmissionsEmptyHintSendable())
        }

        @Test
        fun `should decline the submission and contest terms in task submissions empty hint closed`() {
            assertEquals("Посылок в этом Туре нет.", l.pageParticipant.taskSubmissionsEmptyHintClosed())
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

    @Nested
    inner class PageAdministratorTests {

        @Test
        fun `should use the cabinet and administrator terms in title`() {
            assertEquals("Кабинет Администратора", l.pageAdministrator.title())
        }

        @Test
        fun `should decline the community, user, cabinet and administrator terms in guide`() {
            assertEquals(
                "Управляйте своими Сообществами и их Пользователями через вкладки Кабинета Администратора.",
                l.pageAdministrator.guide(),
            )
        }

        @Test
        fun `should name the identifier in id`() {
            assertEquals("ID", l.pageAdministrator.id())
        }

        @Test
        fun `should name the name in name`() {
            assertEquals("Название", l.pageAdministrator.name())
        }

        @Test
        fun `should name the description in description`() {
            assertEquals("Описание", l.pageAdministrator.description())
        }

        @Test
        fun `should ask for the name in name required`() {
            assertEquals("Укажите название", l.pageAdministrator.nameRequired())
        }

        @Test
        fun `should name the creation action in create`() {
            assertEquals("Создать", l.pageAdministrator.create())
        }

        @Test
        fun `should head the details in details`() {
            assertEquals("Сведения", l.pageAdministrator.details())
        }

        @Test
        fun `should use the nickname term in nickname`() {
            assertEquals("Псевдоним", l.pageAdministrator.nickname())
        }

        @Test
        fun `should use the plural role term in roles`() {
            assertEquals("Роли", l.pageAdministrator.roles())
        }

        @Test
        fun `should name the last sign-in in last login`() {
            assertEquals("Последний вход", l.pageAdministrator.lastLogin())
        }

        @Test
        fun `should use the access code term in access code`() {
            assertEquals("Код-доступа", l.pageAdministrator.accessCode())
        }

        @Test
        fun `should use the prepositional cabinet term in communities guide`() {
            assertEquals("Сообщества, доступные в этом Кабинете.", l.pageAdministrator.communitiesGuide())
        }

        @Test
        fun `should use the genitive plural user term in communities users`() {
            assertEquals("Пользователей", l.pageAdministrator.communitiesUsers())
        }

        @Test
        fun `should use the genitive plural community term in communities empty`() {
            assertEquals("Сообществ пока нет", l.pageAdministrator.communitiesEmpty())
        }

        @Test
        fun `should use the accusative community and user terms in communities empty hint`() {
            assertEquals(
                "Создайте Сообщество и пригласите в него Пользователей.",
                l.pageAdministrator.communitiesEmptyHint(),
            )
        }

        @Test
        fun `should use the community term in communities dialog title`() {
            assertEquals("Новое Сообщество", l.pageAdministrator.communitiesDialogTitle())
        }

        @Test
        fun `should use the community term in communities created`() {
            assertEquals("Сообщество создано", l.pageAdministrator.communitiesCreated())
        }

        @Test
        fun `should use the accusative community term in communities create`() {
            assertEquals("Создать Сообщество", l.pageAdministrator.communitiesCreate())
        }

        @Test
        fun `should decline the user and community terms in users guide`() {
            assertEquals(
                "Пользователи доступных вам Сообществ. Фильтры помогают найти нужную запись.",
                l.pageAdministrator.usersGuide(),
            )
        }

        @Test
        fun `should use the genitive plural user term in users empty`() {
            assertEquals("Пользователей нет", l.pageAdministrator.usersEmpty())
        }

        @Test
        fun `should use the dative community term in users empty hint`() {
            assertEquals(
                "Пользователи появятся, когда присоединятся к Сообществу.",
                l.pageAdministrator.usersEmptyHint(),
            )
        }

        @Test
        fun `should use the genitive nickname term in users nickname hint`() {
            assertEquals("Часть Псевдонима, без учёта регистра", l.pageAdministrator.usersNicknameHint())
        }

        @Test
        fun `should use the community term in users community`() {
            assertEquals("Сообщество", l.pageAdministrator.usersCommunity())
        }

        @Test
        fun `should use the community term in community title`() {
            assertEquals("Сообщество", l.pageAdministrator.communityTitle())
        }

        @Test
        fun `should quote the community name in community head`() {
            assertEquals("Сообщество «Робототехника»", l.pageAdministrator.communityHead(name = "Робототехника"))
        }

        @Test
        fun `should decline the community and user terms in community guide`() {
            assertEquals(
                "Сведения о Сообществе, приглашения и Пользователи собраны на этой странице.",
                l.pageAdministrator.communityGuide(),
            )
        }

        @Test
        fun `should quote the edit button in community details guide`() {
            assertEquals(
                "Просмотрите параметры. Нажмите «Изменить», чтобы отредактировать их, затем сохраните изменения " +
                    "или отмените их.",
                l.pageAdministrator.communityDetailsGuide(),
            )
        }

        @Test
        fun `should use the community term in community edited`() {
            assertEquals("Сообщество изменено", l.pageAdministrator.communityEdited())
        }

        @Test
        fun `should use the genitive community term in community users title`() {
            assertEquals("Пользователи Сообщества", l.pageAdministrator.communityUsersTitle())
        }

        @Test
        fun `should decline the user, community and role terms in community users guide`() {
            assertEquals("Пользователи этого Сообщества и их Роли.", l.pageAdministrator.communityUsersGuide())
        }

        @Test
        fun `should use the plural invite code term in community invites title`() {
            assertEquals("Коды-приглашения", l.pageAdministrator.communityInvitesTitle())
        }

        @Test
        fun `should decline the community and role terms in community invites guide`() {
            assertEquals(
                "Коды для присоединения к Сообществу с указанными Ролями и сроками действия.",
                l.pageAdministrator.communityInvitesGuide(),
            )
        }

        @Test
        fun `should use the role term in community invites role`() {
            assertEquals("Роль", l.pageAdministrator.communityInvitesRole())
        }

        @Test
        fun `should use the invite code term in community invites code`() {
            assertEquals("Код-приглашение", l.pageAdministrator.communityInvitesCode())
        }

        @Test
        fun `should name the expiry in community invites expires`() {
            assertEquals("Действует до", l.pageAdministrator.communityInvitesExpires())
        }

        @Test
        fun `should keep the role name nominative after the instrumental invite code in community invites actions`() {
            assertEquals(
                "Действия с Кодом-приглашением для Роли Разработчик",
                l.pageAdministrator.communityInvitesActions(role = "Разработчик"),
            )
        }

        @Test
        fun `should name the replacing action in community invites replace`() {
            assertEquals("Заменить", l.pageAdministrator.communityInvitesReplace())
        }

        @Test
        fun `should use the accusative invite code term in community invites replace title`() {
            assertEquals("Заменить Код-приглашение?", l.pageAdministrator.communityInvitesReplaceTitle())
        }

        @Test
        fun `should use the invite code term in community invites replace text`() {
            assertEquals("Прежний Код-приглашение перестанет действовать.", l.pageAdministrator.communityInvitesReplaceText())
        }

        @Test
        fun `should agree with the masculine invite code term in community invites replaced`() {
            assertEquals("Код-приглашение заменён", l.pageAdministrator.communityInvitesReplaced())
        }

        @Test
        fun `should name the extending action in community invites extend`() {
            assertEquals("Продлить", l.pageAdministrator.communityInvitesExtend())
        }

        @Test
        fun `should use the genitive invite code term in community invites extended`() {
            assertEquals("Срок действия Кода-приглашения продлён", l.pageAdministrator.communityInvitesExtended())
        }

        @Test
        fun `should use the accusative observer term in community observer create`() {
            assertEquals("Создать Наблюдателя", l.pageAdministrator.communityObserverCreate())
        }

        @Test
        fun `should use the observer term in community observer dialog title`() {
            assertEquals("Новый Наблюдатель", l.pageAdministrator.communityObserverDialogTitle())
        }

        @Test
        fun `should use the genitive plural contest term in community observer dialog subtitle`() {
            assertEquals(
                "Наблюдатель видит результаты выбранных Туров",
                l.pageAdministrator.communityObserverDialogSubtitle(),
            )
        }

        @Test
        fun `should name the observer name in community observer name`() {
            assertEquals("Имя", l.pageAdministrator.communityObserverName())
        }

        @Test
        fun `should ask for the name in community observer name required`() {
            assertEquals("Укажите имя", l.pageAdministrator.communityObserverNameRequired())
        }

        @Test
        fun `should use the plural contest term in community observer contests`() {
            assertEquals("Туры", l.pageAdministrator.communityObserverContests())
        }

        @Test
        fun `should use the dative community term in community observer contests hint`() {
            assertEquals("Туры, открытые Сообществу", l.pageAdministrator.communityObserverContestsHint())
        }

        @Test
        fun `should use the accusative plural contest term in community observer contests required`() {
            assertEquals("Выберите Туры", l.pageAdministrator.communityObserverContestsRequired())
        }

        @Test
        fun `should use the observer term in community observer created`() {
            assertEquals("Наблюдатель создан", l.pageAdministrator.communityObserverCreated())
        }

        @Test
        fun `should quote the observer name in community observer created title`() {
            assertEquals(
                "Наблюдатель «Жюри города» создан",
                l.pageAdministrator.communityObserverCreatedTitle(name = "Жюри города"),
            )
        }

        @Test
        fun `should decline the access code and observer terms in community observer created guide`() {
            assertEquals(
                "Сохраните Код-доступа созданного Наблюдателя и передайте его владельцу.",
                l.pageAdministrator.communityObserverCreatedGuide(),
            )
        }

        @Test
        fun `should use the accusative access code term in community observer alert title`() {
            assertEquals("Сохраните Код-доступа", l.pageAdministrator.communityObserverAlertTitle())
        }

        @Test
        fun `should use the accusative system term in community observer alert text`() {
            assertEquals("Наблюдатель входит по нему в Систему.", l.pageAdministrator.communityObserverAlertText())
        }

        @Test
        fun `should use the accusative observer term in community observer open`() {
            assertEquals("Открыть Наблюдателя", l.pageAdministrator.communityObserverOpen())
        }

        @Test
        fun `should use the user term in user title`() {
            assertEquals("Пользователь", l.pageAdministrator.userTitle())
        }

        @ParameterizedTest
        @CsvSource(
            "USER,     Пользователь «Маша»",
            "OBSERVER, Наблюдатель «Маша»",
            "OTHER,    Пользователь «Маша»",
        )
        fun `should name the user kind and quote the nickname in user head`(kind: UserHeadKind, expected: String) {
            assertEquals(expected, l.pageAdministrator.userHead(kind = kind, name = "Маша"))
        }

        @Test
        fun `should decline the user and role terms in user guide`() {
            assertEquals(
                "Здесь показаны сведения о Пользователе, его Роли и доступная вам статистика.",
                l.pageAdministrator.userGuide(),
            )
        }

        @Test
        fun `should decline the nickname, administrator and user terms in user details guide`() {
            assertEquals(
                "Идентификатор, Псевдоним и другие доступные Администратору сведения о Пользователе.",
                l.pageAdministrator.userDetailsGuide(),
            )
        }

        @Test
        fun `should use the community term in user community`() {
            assertEquals("Сообщество", l.pageAdministrator.userCommunity())
        }

        @Test
        fun `should show the role name after the role term in user role`() {
            assertEquals("Роль: Организатор.", l.pageAdministrator.userRole(role = "Организатор"))
        }

        @Test
        fun `should use the plural solution term in user solutions`() {
            assertEquals("Решения", l.pageAdministrator.userSolutions())
        }

        @Test
        fun `should use the accusative observer term in user observer delete`() {
            assertEquals("Удалить Наблюдателя", l.pageAdministrator.userObserverDelete())
        }

        @Test
        fun `should quote the observer name in user observer delete title`() {
            assertEquals(
                "Удалить Наблюдателя «Жюри города»?",
                l.pageAdministrator.userObserverDeleteTitle(name = "Жюри города"),
            )
        }

        @Test
        fun `should use the genitive observer term in user observer delete text`() {
            assertEquals("Код-доступа Наблюдателя перестанет действовать.", l.pageAdministrator.userObserverDeleteText())
        }

        @Test
        fun `should name the deleting action in user observer delete action`() {
            assertEquals("Удалить", l.pageAdministrator.userObserverDeleteAction())
        }

        @Test
        fun `should use the observer term in user observer deleted`() {
            assertEquals("Наблюдатель удалён", l.pageAdministrator.userObserverDeleted())
        }

        @Test
        fun `should use the prepositional plural community term in user roles title`() {
            assertEquals("Роли в Сообществах", l.pageAdministrator.userRolesTitle())
        }

        @Test
        fun `should decline the community, user and role terms in user roles guide`() {
            assertEquals(
                "Сообщества Пользователя и Роли, которые он выполняет в каждом из них.",
                l.pageAdministrator.userRolesGuide(),
            )
        }

        @Test
        fun `should quote the community name in user roles actions`() {
            assertEquals(
                "Роли в Сообществе «Робототехника»",
                l.pageAdministrator.userRolesActions(community = "Робототехника"),
            )
        }

        @Test
        fun `should use the genitive role and prepositional community terms in user roles empty`() {
            assertEquals("Ролей в ваших Сообществах нет", l.pageAdministrator.userRolesEmpty())
        }

        @Test
        fun `should use the accusative community term in user roles grant`() {
            assertEquals("Включить в Сообщество", l.pageAdministrator.userRolesGrant())
        }

        @Test
        fun `should use the genitive plural role term in user roles nothing to remove`() {
            assertEquals("Нет Ролей для исключения", l.pageAdministrator.userRolesNothingToRemove())
        }

        @Test
        fun `should keep the role name nominative after the prepositional role term in user roles remove`() {
            assertEquals("Исключить в Роли Разработчик", l.pageAdministrator.userRolesRemove(role = "Разработчик"))
        }

        @Test
        fun `should quote the nickname and community name in user roles remove title`() {
            assertEquals(
                "Исключить «Маша» из Сообщества «Робототехника» в Роли Разработчик?",
                l.pageAdministrator.userRolesRemoveTitle(user = "Маша", community = "Робототехника", role = "Разработчик"),
            )
        }

        @Test
        fun `should decline the user, community and role terms in user roles remove text`() {
            assertEquals(
                "Пользователь потеряет доступ к тому, что было доступно ему только через это Сообщество " +
                    "в этой Роли.",
                l.pageAdministrator.userRolesRemoveText(),
            )
        }

        @Test
        fun `should name the removing action in user roles remove action`() {
            assertEquals("Исключить", l.pageAdministrator.userRolesRemoveAction())
        }

        @Test
        fun `should use the genitive community term in user roles removed`() {
            assertEquals("Пользователь исключён из Сообщества", l.pageAdministrator.userRolesRemoved())
        }

        @Test
        fun `should use the genitive plural task term in user tasks guide`() {
            assertEquals("Список Задач и их основные сведения.", l.pageAdministrator.userTasksGuide())
        }

        @Test
        fun `should name the validation status in user tasks status`() {
            assertEquals("Статус проверки", l.pageAdministrator.userTasksStatus())
        }

        @Test
        fun `should use the plural community term in user tasks communities`() {
            assertEquals("Сообщества", l.pageAdministrator.userTasksCommunities())
        }

        @Test
        fun `should use the genitive plural task term in user tasks empty`() {
            assertEquals("Задач нет", l.pageAdministrator.userTasksEmpty())
        }

        @Test
        fun `should decline the contest, user, task and solution terms in user contests guide`() {
            assertEquals(
                "Туры Пользователя, число Задач и количество Решений по каждой Задаче.",
                l.pageAdministrator.userContestsGuide(),
            )
        }

        @Test
        fun `should use the plural task term in user contests tasks`() {
            assertEquals("Задачи", l.pageAdministrator.userContestsTasks())
        }

        @Test
        fun `should use the dative plural task term in user contests solutions`() {
            assertEquals("Решения по Задачам", l.pageAdministrator.userContestsSolutions())
        }

        @Test
        fun `should use the genitive plural contest term in user contests empty`() {
            assertEquals("Туров нет", l.pageAdministrator.userContestsEmpty())
        }

        @Test
        fun `should use the genitive plural class term in user classes guide`() {
            assertEquals("Список Классов и сведения о них.", l.pageAdministrator.userClassesGuide())
        }

        @Test
        fun `should use the plural student term in user classes students`() {
            assertEquals("Ученики", l.pageAdministrator.userClassesStudents())
        }

        @Test
        fun `should use the instrumental plural solution term in user classes authors`() {
            assertEquals("Ученики с Решениями", l.pageAdministrator.userClassesAuthors())
        }

        @Test
        fun `should use the genitive plural class term in user classes empty`() {
            assertEquals("Классов нет", l.pageAdministrator.userClassesEmpty())
        }

        @Test
        fun `should use the genitive plural competition term in user competitions guide`() {
            assertEquals("Список Соревнований и сведения о них.", l.pageAdministrator.userCompetitionsGuide())
        }

        @Test
        fun `should use the plural participant term in user competitions participants`() {
            assertEquals("Участники", l.pageAdministrator.userCompetitionsParticipants())
        }

        @Test
        fun `should use the instrumental plural solution term in user competitions authors`() {
            assertEquals("Участники с Решениями", l.pageAdministrator.userCompetitionsAuthors())
        }

        @Test
        fun `should use the genitive plural competition term in user competitions empty`() {
            assertEquals("Соревнований нет", l.pageAdministrator.userCompetitionsEmpty())
        }

        @Test
        fun `should use the plural judgment order term in user judgments title`() {
            assertEquals("Судейские вердикты", l.pageAdministrator.userJudgmentsTitle())
        }

        @Test
        fun `should keep the partial verdict reference lowercase in user judgments guide`() {
            assertEquals("История выставленных Судьями вердиктов.", l.pageAdministrator.userJudgmentsGuide())
        }

        @Test
        fun `should use the solution term in user judgments solution`() {
            assertEquals("Решение", l.pageAdministrator.userJudgmentsSolution())
        }

        @Test
        fun `should name the issue time in user judgments issued`() {
            assertEquals("Выставлен", l.pageAdministrator.userJudgmentsIssued())
        }

        @Test
        fun `should name the previous result in user judgments previous result`() {
            assertEquals("Предыдущий результат", l.pageAdministrator.userJudgmentsPreviousResult())
        }

        @Test
        fun `should name the new result in user judgments new result`() {
            assertEquals("Новый результат", l.pageAdministrator.userJudgmentsNewResult())
        }

        @Test
        fun `should use the genitive plural judgment order term in user judgments empty`() {
            assertEquals("Судейских вердиктов нет", l.pageAdministrator.userJudgmentsEmpty())
        }

        @Test
        fun `should use the plural contest term in user assigned title`() {
            assertEquals("Назначенные Туры", l.pageAdministrator.userAssignedTitle())
        }

        @Test
        fun `should decline the contest, user and competition terms in user assigned guide`() {
            assertEquals(
                "Туры, назначенные Пользователю, и связанные Соревнования.",
                l.pageAdministrator.userAssignedGuide(),
            )
        }

        @Test
        fun `should use the contest term in user assigned contest`() {
            assertEquals("Тур", l.pageAdministrator.userAssignedContest())
        }

        @Test
        fun `should use the plural competition term in user assigned competitions`() {
            assertEquals("Соревнования", l.pageAdministrator.userAssignedCompetitions())
        }

        @Test
        fun `should use the genitive plural contest term in user assigned empty`() {
            assertEquals("Назначенных Туров нет", l.pageAdministrator.userAssignedEmpty())
        }

        @Test
        fun `should use the accusative community term in user grant title`() {
            assertEquals("Включение в Сообщество", l.pageAdministrator.userGrantTitle())
        }

        @Test
        fun `should use the accusative community term in user grant community required`() {
            assertEquals("Выберите Сообщество", l.pageAdministrator.userGrantCommunityRequired())
        }

        @Test
        fun `should use the role term in user grant role`() {
            assertEquals("Роль", l.pageAdministrator.userGrantRole())
        }

        @Test
        fun `should use the accusative role term in user grant role required`() {
            assertEquals("Выберите Роль", l.pageAdministrator.userGrantRoleRequired())
        }

        @Test
        fun `should name the granting action in user grant submit`() {
            assertEquals("Включить", l.pageAdministrator.userGrantSubmit())
        }

        @Test
        fun `should use the user and accusative community terms in user grant granted`() {
            assertEquals("Пользователь включён в Сообщество", l.pageAdministrator.userGrantGranted())
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

        @ParameterizedTest
        @CsvSource(
            "PYTHON,          Python",
            "JAVASCRIPT,      JavaScript",
            "VISUAL_LANGUAGE, Визуальный язык",
            "OTHER,           Неизвестный язык",
        )
        fun `should name the language in language name`(language: LanguageNameLanguage, expected: String) {
            assertEquals(expected, l.pageDeveloper.languageName(language = language))
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

    @Nested
    inner class PageObserverTests {

        @Test
        fun `should name the observer cabinet in title`() {
            assertEquals("Кабинет Наблюдателя", l.pageObserver.title())
        }

        @Test
        fun `should decline the contest and competition terms in guide`() {
            assertEquals(
                "Просматривайте назначенные вам Туры Соревнований и скачивайте все результаты одним файлом.",
                l.pageObserver.guide(),
            )
        }

        @Test
        fun `should use the competition term in competition`() {
            assertEquals("Соревнование", l.pageObserver.competition())
        }

        @Test
        fun `should use the contest term in contest`() {
            assertEquals("Тур", l.pageObserver.contest())
        }

        @Test
        fun `should name the start in starts at`() {
            assertEquals("Начало", l.pageObserver.startsAt())
        }

        @Test
        fun `should name the end in ends at`() {
            assertEquals("Конец", l.pageObserver.endsAt())
        }

        @Test
        fun `should name the time to pass in attempt duration`() {
            assertEquals("Время на прохождение", l.pageObserver.attemptDuration())
        }

        @Test
        fun `should use the genitive competition term in filter competition id`() {
            assertEquals("ID Соревнования", l.pageObserver.filterCompetitionId())
        }

        @Test
        fun `should use the genitive contest term in filter contest id`() {
            assertEquals("ID Тура", l.pageObserver.filterContestId())
        }

        @Test
        fun `should use the plural contest and genitive plural competition terms in contests title`() {
            assertEquals("Туры Соревнований", l.pageObserver.contestsTitle())
        }

        @Test
        fun `should quote the download button in contests guide`() {
            assertEquals(
                "Каждая строка — Соревнование и назначенный вам Тур с датами и временем на прохождение. " +
                    "Фильтры сужают список. Кнопка «Скачать все результаты» выгружает один CSV-файл с результатами " +
                    "всех доступных вам Соревнований независимо от фильтров.",
                l.pageObserver.contestsGuide(),
            )
        }

        @Test
        fun `should use the genitive plural contest term in contests empty`() {
            assertEquals("Туров пока нет", l.pageObserver.contestsEmpty())
        }

        @Test
        fun `should use the instrumental plural competition term in contests empty hint`() {
            assertEquals(
                "Назначенные Туры появятся здесь вместе с Соревнованиями.",
                l.pageObserver.contestsEmptyHint(),
            )
        }

        @Test
        fun `should name the download action in contests download`() {
            assertEquals("Скачать все результаты", l.pageObserver.contestsDownload())
        }

        @Test
        fun `should keep the lowercase participant in csv participant id`() {
            assertEquals("ID участника", l.pageObserver.csvParticipantId())
        }

        @Test
        fun `should join the contest and task names with a spaced hyphen in csv task`() {
            assertEquals("Финал - Робот", l.pageObserver.csvTask(contest = "Финал", task = "Робот"))
        }
    }

    @Nested
    inner class PageSupervisorTests {

        @Test
        fun `should name the supervisor cabinet in title`() {
            assertEquals("Кабинет Супервайзера", l.pageSupervisor.title())
        }

        @Test
        fun `should decline the user, system, supervisor, community and cabinet terms in guide`() {
            assertEquals(
                "Все Пользователи Системы, кроме Супервайзеров, и все Сообщества собраны во вкладках " +
                    "Кабинета Супервайзера.",
                l.pageSupervisor.guide(),
            )
        }

        @Test
        fun `should name the identifier in id`() {
            assertEquals("ID", l.pageSupervisor.id())
        }

        @Test
        fun `should name the name in name`() {
            assertEquals("Название", l.pageSupervisor.name())
        }

        @Test
        fun `should name the description in description`() {
            assertEquals("Описание", l.pageSupervisor.description())
        }

        @Test
        fun `should head the details in details`() {
            assertEquals("Сведения", l.pageSupervisor.details())
        }

        @Test
        fun `should use the nickname term in nickname`() {
            assertEquals("Псевдоним", l.pageSupervisor.nickname())
        }

        @Test
        fun `should use the plural role term in roles`() {
            assertEquals("Роли", l.pageSupervisor.roles())
        }

        @Test
        fun `should name the last sign-in in last login`() {
            assertEquals("Последний вход", l.pageSupervisor.lastLogin())
        }

        @Test
        fun `should use the access code term in access code`() {
            assertEquals("Код-доступа", l.pageSupervisor.accessCode())
        }

        @Test
        fun `should decline the user, system and supervisor terms in users guide`() {
            assertEquals(
                "Все Пользователи Системы, кроме Супервайзеров. Фильтры помогают найти нужную запись. " +
                    "Выберите строку, чтобы открыть сведения о Пользователе.",
                l.pageSupervisor.usersGuide(),
            )
        }

        @Test
        fun `should decline the community and system terms in communities guide`() {
            assertEquals(
                "Все Сообщества Системы. Выберите строку, чтобы открыть Сообщество.",
                l.pageSupervisor.communitiesGuide(),
            )
        }

        @Test
        fun `should use the genitive plural user term in communities users`() {
            assertEquals("Пользователей", l.pageSupervisor.communitiesUsers())
        }

        @Test
        fun `should use the genitive plural community term in communities empty`() {
            assertEquals("Сообществ пока нет", l.pageSupervisor.communitiesEmpty())
        }

        @Test
        fun `should use the user term in user title`() {
            assertEquals("Пользователь", l.pageSupervisor.userTitle())
        }

        @ParameterizedTest
        @CsvSource(
            "USER,        Пользователь «Маша»",
            "OBSERVER,    Наблюдатель «Маша»",
            "PARTICIPANT, Участник «Маша»",
            "OTHER,       Пользователь «Маша»",
        )
        fun `should name the user kind and quote the nickname in user head`(kind: PageSupervisor.UserHeadKind, expected: String) {
            assertEquals(expected, l.pageSupervisor.userHead(kind = kind, name = "Маша"))
        }

        @Test
        fun `should decline the user, role and supervisor terms in user guide`() {
            assertEquals(
                "Здесь показаны сведения о Пользователе, его Роли и связанные с ним данные, доступные Супервайзеру.",
                l.pageSupervisor.userGuide(),
            )
        }

        @Test
        fun `should use the instrumental user term in user impersonate`() {
            assertEquals("Войти под Пользователем", l.pageSupervisor.userImpersonate())
        }

        @Test
        fun `should decline the nickname, access code and user terms in user details guide`() {
            assertEquals(
                "Идентификатор, Псевдоним, время последнего входа и Код-доступа Пользователя.",
                l.pageSupervisor.userDetailsGuide(),
            )
        }

        @Test
        fun `should show the role name after the role term in user role`() {
            assertEquals("Роль: Организатор.", l.pageSupervisor.userRole(role = "Организатор"))
        }

        @Test
        fun `should use the prepositional plural community term in user roles title`() {
            assertEquals("Роли в Сообществах", l.pageSupervisor.userRolesTitle())
        }

        @Test
        fun `should decline the community, user and role terms in user roles guide`() {
            assertEquals(
                "Сообщества Пользователя и Роли, которые он выполняет в каждом из них.",
                l.pageSupervisor.userRolesGuide(),
            )
        }

        @Test
        fun `should use the community term in user roles community`() {
            assertEquals("Сообщество", l.pageSupervisor.userRolesCommunity())
        }

        @Test
        fun `should quote the community name in user roles actions`() {
            assertEquals(
                "Роли в Сообществе «Робототехника»",
                l.pageSupervisor.userRolesActions(community = "Робототехника"),
            )
        }

        @Test
        fun `should use the genitive role and prepositional community terms in user roles empty`() {
            assertEquals("Ролей в Сообществах нет", l.pageSupervisor.userRolesEmpty())
        }

        @Test
        fun `should use the accusative community term in user roles grant`() {
            assertEquals("Включить в Сообщество", l.pageSupervisor.userRolesGrant())
        }

        @Test
        fun `should use the genitive plural role term in user roles nothing to remove`() {
            assertEquals("Нет Ролей для исключения", l.pageSupervisor.userRolesNothingToRemove())
        }

        @Test
        fun `should keep the role name nominative after the prepositional role term in user roles remove`() {
            assertEquals("Исключить в Роли Разработчик", l.pageSupervisor.userRolesRemove(role = "Разработчик"))
        }

        @Test
        fun `should quote the nickname and community name in user roles remove title`() {
            assertEquals(
                "Исключить «Маша» из Сообщества «Робототехника» в Роли Разработчик?",
                l.pageSupervisor.userRolesRemoveTitle(user = "Маша", community = "Робототехника", role = "Разработчик"),
            )
        }

        @Test
        fun `should decline the user, community and role terms in user roles remove text`() {
            assertEquals(
                "Пользователь потеряет доступ к тому, что было доступно ему только через это Сообщество " +
                    "в этой Роли.",
                l.pageSupervisor.userRolesRemoveText(),
            )
        }

        @Test
        fun `should name the removing action in user roles remove action`() {
            assertEquals("Исключить", l.pageSupervisor.userRolesRemoveAction())
        }

        @Test
        fun `should use the genitive community term in user roles removed`() {
            assertEquals("Пользователь исключён из Сообщества", l.pageSupervisor.userRolesRemoved())
        }

        @Test
        fun `should use the instrumental administrator term in user administrator grant`() {
            assertEquals("Назначить Администратором", l.pageSupervisor.userAdministratorGrant())
        }

        @Test
        fun `should quote the nickname in user administrator grant title`() {
            assertEquals(
                "Назначить «Маша» Администратором?",
                l.pageSupervisor.userAdministratorGrantTitle(name = "Маша"),
            )
        }

        @Test
        fun `should decline the system, community, role and administrator terms in user administrator grant text`() {
            assertEquals(
                "Система создаст Сообщество Администратора. Снять Роль Администратора нельзя.",
                l.pageSupervisor.userAdministratorGrantText(),
            )
        }

        @Test
        fun `should name the granting action in user administrator grant action`() {
            assertEquals("Назначить", l.pageSupervisor.userAdministratorGrantAction())
        }

        @Test
        fun `should use the user and instrumental administrator terms in user administrator granted`() {
            assertEquals("Пользователь назначен Администратором", l.pageSupervisor.userAdministratorGranted())
        }

        @Test
        fun `should use the accusative community term in user grant title`() {
            assertEquals("Включение в Сообщество", l.pageSupervisor.userGrantTitle())
        }

        @Test
        fun `should use the community term in user grant community`() {
            assertEquals("Сообщество", l.pageSupervisor.userGrantCommunity())
        }

        @Test
        fun `should use the accusative community term in user grant community required`() {
            assertEquals("Выберите Сообщество", l.pageSupervisor.userGrantCommunityRequired())
        }

        @Test
        fun `should use the role term in user grant role`() {
            assertEquals("Роль", l.pageSupervisor.userGrantRole())
        }

        @Test
        fun `should use the accusative role term in user grant role required`() {
            assertEquals("Выберите Роль", l.pageSupervisor.userGrantRoleRequired())
        }

        @Test
        fun `should name the granting action in user grant submit`() {
            assertEquals("Включить", l.pageSupervisor.userGrantSubmit())
        }

        @Test
        fun `should use the user and accusative community terms in user grant granted`() {
            assertEquals("Пользователь включён в Сообщество", l.pageSupervisor.userGrantGranted())
        }

        @Test
        fun `should use the community term in user community title`() {
            assertEquals("Сообщество", l.pageSupervisor.userCommunityTitle())
        }

        @Test
        fun `should use the community and observer terms in user community guide`() {
            assertEquals("Сообщество, в котором создан Наблюдатель.", l.pageSupervisor.userCommunityGuide())
        }

        @Test
        fun `should use the competition term in user participant title`() {
            assertEquals("Соревнование", l.pageSupervisor.userParticipantTitle())
        }

        @Test
        fun `should use the competition and participant terms in user participant guide`() {
            assertEquals(
                "Соревнование, к которому относится Участник.",
                l.pageSupervisor.userParticipantGuide(),
            )
        }
    }

    private companion object {
        // ICU digit groups and some messages use a no-break space; the code point keeps the expectations visible.
        val NBSP = Char(0x00A0).toString()
    }
}
