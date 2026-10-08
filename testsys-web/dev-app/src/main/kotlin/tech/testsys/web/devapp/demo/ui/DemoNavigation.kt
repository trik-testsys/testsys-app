package tech.testsys.web.devapp.demo.ui

import tech.testsys.web.devapp.demo.model.DemoRole

/** Role navigation and sections of the existing demonstration. */
internal data class DemoCabinet(
    val key: String,
    val role: DemoRole,
    val sections: List<Pair<String, String>>,
    val label: String,
)

internal val DEMO_CABINETS: List<DemoCabinet> = listOf(
    DemoCabinet(
        key = "student",
        role = DemoRole.Student,
        label = "Кабинет ученика",
        sections = listOf(
            "overview" to "Обзор",
            "profile" to "Профиль",
            "study" to "Классы и туры",
            "solutions" to "Решения",
        ),
    ),
    DemoCabinet(
        key = "organizer",
        role = DemoRole.Organizer,
        label = "Кабинет организатора",
        sections = listOf(
            "overview" to "Обзор",
            "competitions" to "Соревнования",
            "participants" to "Участники",
            "tours" to "Туры",
            "results" to "Результаты",
        ),
    ),
    DemoCabinet(
        key = "developer",
        role = DemoRole.Developer,
        label = "Кабинет разработчика",
        sections = listOf(
            "overview" to "Обзор",
            "resources" to "Ресурсы",
            "tasks" to "Задачи",
            "tours" to "Туры",
        ),
    ),
    DemoCabinet(
        key = "judge",
        role = DemoRole.Judge,
        label = "Кабинет судьи",
        sections = listOf(
            "overview" to "Обзор",
            "solutions" to "Решения",
            "result" to "Результат проверки",
        ),
    ),
    DemoCabinet(
        key = "administrator",
        role = DemoRole.Administrator,
        label = "Кабинет администратора",
        sections = listOf(
            "overview" to "Обзор",
            "users" to "Пользователи",
            "user" to "Сведения о пользователе",
        ),
    ),
    DemoCabinet(
        key = "participant",
        role = DemoRole.Participant,
        label = "Кабинет участника",
        sections = listOf(
            "overview" to "Обзор",
            "tours" to "Доступные туры",
            "solutions" to "Задачи и решения",
        ),
    ),
    DemoCabinet(
        key = "observer",
        role = DemoRole.Observer,
        label = "Кабинет наблюдателя",
        sections = listOf(
            "overview" to "Обзор",
            "tours" to "Доступные туры",
            "results" to "Результаты",
        ),
    ),
    DemoCabinet(
        key = "supervisor",
        role = DemoRole.Supervisor,
        label = "Кабинет супервайзера",
        sections = listOf(
            "overview" to "Обзор",
            "purpose" to "Назначение",
            "capabilities" to "Возможности",
        ),
    ),
)

/** Resolves only declared demo keys; unknown keys display the home screen. */
internal fun demoScreen(value: String?): String = when {
    value == "login" -> "login"
    value == "profile" -> "student.overview"
    value == "org" -> "organizer.overview"
    DEMO_CABINETS.any { cabinet ->
        cabinet.sections.any { (section, _) ->
            "${cabinet.key}.$section" == value
        }
    }
    -> checkNotNull(value) { "Declared demo screen key must not be null" }
    else -> "home"
}
