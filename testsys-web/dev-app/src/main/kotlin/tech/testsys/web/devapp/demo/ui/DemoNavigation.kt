package tech.testsys.web.devapp.demo.ui

/** Role navigation and sections of the existing demonstration. */
internal data class DemoCabinet(
    val key: String,
    val role: String,
    val sections: List<Pair<String, String>>,
    val label: String = role,
)

internal val DEMO_CABINETS: List<DemoCabinet> = listOf(
    DemoCabinet(
        key = "student",
        role = "Ученик",
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
        role = "Организатор",
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
        role = "Разработчик",
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
        role = "Судья",
        label = "Кабинет судьи",
        sections = listOf(
            "overview" to "Обзор",
            "solutions" to "Решения",
            "result" to "Результат проверки",
        ),
    ),
    DemoCabinet(
        key = "administrator",
        role = "Администратор",
        label = "Кабинет администратора",
        sections = listOf(
            "overview" to "Обзор",
            "users" to "Пользователи",
            "user" to "Сведения о пользователе",
        ),
    ),
    DemoCabinet(
        key = "participant",
        role = "Участник",
        label = "Кабинет участника",
        sections = listOf(
            "overview" to "Обзор",
            "tours" to "Доступные туры",
            "solutions" to "Задачи и решения",
        ),
    ),
    DemoCabinet(
        key = "observer",
        role = "Наблюдатель",
        label = "Кабинет наблюдателя",
        sections = listOf(
            "overview" to "Обзор",
            "tours" to "Доступные туры",
            "results" to "Результаты",
        ),
    ),
    DemoCabinet(
        key = "supervisor",
        role = "Супервайзер",
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
        cabinet.sections.any {
            "${cabinet.key}.${it.first}" == value
        }
    }
    -> checkNotNull(value)
    else -> "home"
}
