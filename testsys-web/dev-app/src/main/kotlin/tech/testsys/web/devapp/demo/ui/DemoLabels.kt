package tech.testsys.web.devapp.demo.ui

import tech.testsys.web.components.display.Tone

private val SOLUTION_KIND_LABELS = mapOf(
    "VisualLanguage" to "Визуальная программа TRIK Studio",
    "Python" to "Python",
    "JavaScript" to "JavaScript",
)
private val SOLUTION_STATUS_LABELS = mapOf(
    "Queue" to "В очереди",
    "Checking" to "Проверяется",
    "Checked" to "Проверено",
    "Error" to "Ошибка проверки",
    "Timeout" to "Тайм-аут",
)
private val TASK_STATE_LABELS = mapOf(
    "New" to "Новая",
    "Uncommitted" to "Есть незафиксированные изменения",
    "Committed" to "Зафиксирована",
)

/** Display names of existing mock solution formats. */
internal fun demoKindLabel(value: String): String = SOLUTION_KIND_LABELS[value] ?: value

/** Display names of existing mock grading statuses. */
internal fun demoStatusLabel(value: String): String = SOLUTION_STATUS_LABELS[value] ?: value

/** Display names of existing mock task revision states. */
internal fun demoTaskStateLabel(value: String): String = TASK_STATE_LABELS[value] ?: value

/** Status tone remains independent of the neutral numeric score. */
internal fun demoStatusTone(value: String): Tone = if (value in listOf("Error", "Timeout")) Tone.Danger else Tone.Info
