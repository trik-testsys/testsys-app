package tech.testsys.web.service.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingKey
import tech.testsys.infra.settings.api.SettingKind
import tech.testsys.infra.settings.api.SettingsChangeResult
import tech.testsys.infra.settings.api.SettingsPersistence
import tech.testsys.infra.settings.api.SettingsService
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.iconAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.textInput

/**
 * Service-only atomic settings editor, refreshed on navigation and after successful changes.
 *
 * @since %CURRENT_VERSION%
 */
@Route("settings")
@PageTitle("Настройки Системы")
@RolesAllowed("SERVICE")
class ServiceSettingsView(
    private val settings: SettingsService,
    private val runtime: RuntimeSettings,
) : TestSysView(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) = showSettings()

    private fun showSettings() {
        val overview = settings.overview()
        val snapshot = overview.snapshot
        val form = ServiceSettingsForm(snapshot)
        page(ServiceHeaders.header()) {
            head("Настройки Системы")
            row {
                block(title = "Настройки") {
                    if (overview.persistence == SettingsPersistence.Known) {
                        editing(onSave = {
                            val changes = form.changes()
                            if (changes == null) false else applyChanges(overview.revision, changes, form)
                        }, onCancel = form::cancel)
                    } else {
                        row { text("Сохранённое состояние неопределённо. Изменения заблокированы до перезапуска.") }
                    }
                    GROUPS.forEach { (group, keys) ->
                        sectionTitle(group)
                        keys.forEach { key ->
                            row {
                                val input =
                                    textInput(LABELS.getValue(key), labelSize = 10, size = 13, hint = key.property)
                                form.bind(key, input)
                                input.isEditable = overview.persistence == SettingsPersistence.Known
                                horizontal(size = 1) { iconAction(IconName.Info, label = settingHelp(key, snapshot)) }
                            }
                            separator()
                        }
                    }
                    row {
                        textInput("Публичное Сообщество (ID)", labelSize = 10, size = 14) {
                            value = runtime.publicCommunityId.value.toString()
                            isEditable = false
                        }
                    }
                    if (snapshot.values.int(SettingKey.MaxMessageBytes) != snapshot.activeMaxMessageBytes) {
                        row {
                            text(
                                "Размер входящего сообщения: действующий предел — ${snapshot.activeMaxMessageBytes} байт, " +
                                    "сохранённый — ${snapshot.values[SettingKey.MaxMessageBytes]} байт. " +
                                    "Для применения требуется перезапуск.",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applyChanges(revision: Long, changes: Map<SettingKey, String>, form: ServiceSettingsForm): Boolean {
        return when (val result = settings.change(revision, changes)) {
            is SettingsChangeResult.Applied -> {
                toast(FeedbackKind.Success, "Настройки применены")
                showSettings()
                true
            }
            is SettingsChangeResult.Invalid -> {
                form.errors(result.errors)
                false
            }
            SettingsChangeResult.Stale -> {
                toast(FeedbackKind.Error, "Настройки изменились. Обновите страницу браузера и повторите изменения")
                false
            }
            SettingsChangeResult.Failed -> {
                toast(FeedbackKind.Error, "Не удалось применить настройки. Прежние значения сохранены; обновите страницу")
                false
            }
            SettingsChangeResult.Uncertain -> {
                toast(FeedbackKind.Error, "Исход сохранения неопределён. Изменения заблокированы до перезапуска")
                showSettings()
                false
            }
        }
    }

    private fun settingHelp(key: SettingKey, snapshot: SettingsSnapshot): String {
        val activeLimit = if (key.isRestartRequired) " Действующий предел — ${snapshot.activeMaxMessageBytes} байт." else ""
        return "${description(key)}\n\n${inputFormat(key)}.\n\nИзменения вступят в силу ${applicationBoundary(key)}.$activeLimit"
    }

    private fun description(key: SettingKey): String = when (key) {
        SettingKey.MaxParticipants -> "Максимальное общее количество Участников в одном Соревновании."
        SettingKey.ConfirmationCodeLifetime -> "Время, в течение которого можно подтвердить почту выданным кодом."
        SettingKey.MaxConfirmationAttempts -> "Количество попыток ввода кода подтверждения почты."
        SettingKey.CommunityInviteTtl -> "Срок действия Кода-приглашения в Сообщество."
        SettingKey.ClassInviteTtl -> "Срок действия Кода-приглашения в Класс."
        SettingKey.GradingMaxAttempts -> "Общее число транспортных попыток запуска проверки на Проверяющем узле."
        SettingKey.TotalTimeout -> "Общее время на подготовку, ожидание Проверяющего узла и выполнение проверки."
        SettingKey.RpcTimeout -> "Срок одного запроса проверки к узлу; ограничен оставшимся общим временем проверки."
        SettingKey.ShouldRecordVideo -> "Запрашивать у Проверяющего узла видеозапись проверки."
        SettingKey.PollInterval -> "Промежуток между фоновыми опросами состояния Проверяющих узлов."
        SettingKey.StatusTimeout -> "Максимальное время ожидания ответа о состоянии Проверяющего узла."
        SettingKey.MaxMessageBytes -> "Максимальный размер входящего gRPC-сообщения от Проверяющего узла."
        SettingKey.MaxTimeLimitMillis -> "Верхняя граница ограничения времени в Полигоне, которую проверяет Диагностика."
        SettingKey.ScorePrefix -> "Текст, по которому Диагностика распознаёт вывод баллов в Полигоне. Регистр учитывается."
        SettingKey.CleanupMinimumAge -> "Минимальное время после изменения файла без записи в БД, позволяющее удалить его при очистке."
        SettingKey.CleanupBatchSize -> "Максимальное количество записей или файлов в одном пакете очистки."
    }

    private fun inputFormat(key: SettingKey): String = when (key.kind) {
        SettingKind.Integer -> {
            val range = if (key == SettingKey.MaxParticipants) "Неотрицательное" else "Положительное"
            "$range целое число типа Kotlin Int" + if (key == SettingKey.MaxMessageBytes) ", в байтах" else ""
        }
        SettingKind.Long -> "Неотрицательное целое число типа Kotlin Long, в миллисекундах"
        SettingKind.Boolean -> "true — включена, false — выключена"
        SettingKind.Duration -> "Положительная длительность ISO-8601, например PT5M"
        SettingKind.Text -> "Текст не должен быть пустым или состоять только из пробелов; пробелы сохраняются"
    }

    private fun applicationBoundary(key: SettingKey): String = when (key) {
        SettingKey.MaxParticipants -> "при последующих добавлениях Участников; существующие Участники останутся в Соревновании"
        SettingKey.ConfirmationCodeLifetime, SettingKey.MaxConfirmationAttempts ->
            "при создании или перезаписи запроса подтверждения почты"
        SettingKey.CommunityInviteTtl, SettingKey.ClassInviteTtl -> "при создании, замене или продлении Кода-приглашения"
        SettingKey.GradingMaxAttempts, SettingKey.TotalTimeout, SettingKey.RpcTimeout, SettingKey.ShouldRecordVideo ->
            "при приёме новой проверки. Уже принятые проверки сохранят прежние настройки до завершения"
        SettingKey.PollInterval -> "после сохранения: расписание опроса будет перестроено с новым интервалом"
        SettingKey.StatusTimeout -> "при следующем запросе статуса узла"
        SettingKey.MaxMessageBytes -> "после перезапуска приложения. Поле показывает сохранённое значение"
        SettingKey.MaxTimeLimitMillis, SettingKey.ScorePrefix -> "при следующей Диагностике"
        SettingKey.CleanupMinimumAge, SettingKey.CleanupBatchSize ->
            "при следующем явном проходе очистки. Сохранение настройки не запускает очистку"
    }

    private companion object {
        private val GROUPS = listOf(
            "Соревнования" to listOf(SettingKey.MaxParticipants),
            "Подтверждение почты" to listOf(SettingKey.ConfirmationCodeLifetime, SettingKey.MaxConfirmationAttempts),
            "Приглашения" to listOf(SettingKey.CommunityInviteTtl, SettingKey.ClassInviteTtl),
            "Проверка" to listOf(
                SettingKey.GradingMaxAttempts,
                SettingKey.TotalTimeout,
                SettingKey.RpcTimeout,
                SettingKey.ShouldRecordVideo,
                SettingKey.PollInterval,
                SettingKey.StatusTimeout,
                SettingKey.MaxMessageBytes,
            ),
            "Диагностика" to listOf(SettingKey.MaxTimeLimitMillis, SettingKey.ScorePrefix),
            "Очистка файлов" to listOf(SettingKey.CleanupMinimumAge, SettingKey.CleanupBatchSize),
        )

        private val LABELS = mapOf(
            SettingKey.MaxParticipants to "Лимит Участников",
            SettingKey.ConfirmationCodeLifetime to "Срок кода подтверждения",
            SettingKey.MaxConfirmationAttempts to "Попытки подтверждения",
            SettingKey.CommunityInviteTtl to "Срок приглашения в Сообщество",
            SettingKey.ClassInviteTtl to "Срок приглашения в Класс",
            SettingKey.GradingMaxAttempts to "Транспортные попытки",
            SettingKey.TotalTimeout to "Общий срок проверки",
            SettingKey.RpcTimeout to "Предельный срок запроса проверки",
            SettingKey.ShouldRecordVideo to "Видеозапись",
            SettingKey.PollInterval to "Интервал опроса",
            SettingKey.StatusTimeout to "Предельный срок опроса",
            SettingKey.MaxMessageBytes to "Размер входящего сообщения",
            SettingKey.MaxTimeLimitMillis to "Предел времени Диагностики, мс",
            SettingKey.ScorePrefix to "Префикс баллов",
            SettingKey.CleanupMinimumAge to "Возраст файла для очистки",
            SettingKey.CleanupBatchSize to "Размер пакета очистки",
        )
    }
}
