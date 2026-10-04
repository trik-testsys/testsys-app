package tech.testsys.web.devapp.demo

import com.vaadin.flow.component.UI
import com.vaadin.flow.spring.annotation.UIScope
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private const val CHECK_START_DELAY = 500L

private const val CHECK_FINISH_DELAY = 1_500L

/** UI-owned demonstration state; a fresh UI starts from the fixtures. */
@Component
@UIScope
internal class DemoSession {
    var state: DemoState = demoFixtures()
    val selections: MutableMap<String, String> = mutableMapOf()
    val tables: MutableMap<String, DemoTableState> = mutableMapOf()
    private var generation: Long = 0
    fun apply(result: DemoResult): Boolean {
        state = result.state
        return result.isSuccess
    }
    fun check(id: String, render: () -> Unit) {
        val ui = UI.getCurrent()
        val current = generation
        schedule(ui, current, CHECK_START_DELAY) {
            state = state.checkSolution(id)
            render()
        }
        schedule(ui, current, CHECK_FINISH_DELAY) {
            state = state.finishSolution(id = id, status = "Checked", score = 64)
            render()
        }
    }
    private fun schedule(ui: UI, current: Long, delay: Long, action: () -> Unit) {
        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute {
            if (ui.isAttached) {
                ui.access {
                    if (current == generation) {
                        action()
                    }
                }
            }
        }
    }
    fun reset() {
        generation++
        state = demoFixtures()
        selections.clear()
        tables.clear()
    }
}

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
