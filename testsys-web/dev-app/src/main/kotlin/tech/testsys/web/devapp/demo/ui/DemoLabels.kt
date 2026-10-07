package tech.testsys.web.devapp.demo.ui

import tech.testsys.web.components.display.Tone
import tech.testsys.web.devapp.demo.model.DemoRole
import tech.testsys.web.devapp.demo.model.DemoSolutionKind
import tech.testsys.web.devapp.demo.model.DemoSolutionStatus
import tech.testsys.web.devapp.demo.model.DemoTaskState

/** Display names of the demonstration Roles. */
internal fun demoRoleLabel(value: DemoRole): String = when (value) {
    DemoRole.Student -> "Ученик"
    DemoRole.Organizer -> "Организатор"
    DemoRole.Participant -> "Участник"
    DemoRole.Developer -> "Разработчик"
    DemoRole.Judge -> "Судья"
    DemoRole.Administrator -> "Администратор"
    DemoRole.Observer -> "Наблюдатель"
    DemoRole.Supervisor -> "Супервайзер"
}

/** Display names of existing mock solution formats. */
internal fun demoKindLabel(value: DemoSolutionKind): String = when (value) {
    DemoSolutionKind.VisualLanguage -> "Визуальная программа TRIK Studio"
    DemoSolutionKind.Python -> "Python"
    DemoSolutionKind.JavaScript -> "JavaScript"
}

/** Display names of existing mock grading statuses. */
internal fun demoStatusLabel(value: DemoSolutionStatus): String = when (value) {
    DemoSolutionStatus.Queue -> "В очереди"
    DemoSolutionStatus.Checking -> "Проверяется"
    DemoSolutionStatus.Checked -> "Проверено"
    DemoSolutionStatus.Error -> "Ошибка проверки"
    DemoSolutionStatus.Timeout -> "Тайм-аут"
}

/** Display names of existing mock task revision states. */
internal fun demoTaskStateLabel(value: DemoTaskState): String = when (value) {
    DemoTaskState.New -> "Новая"
    DemoTaskState.Uncommitted -> "Есть незафиксированные изменения"
    DemoTaskState.Committed -> "Зафиксирована"
}

/** Status tone remains independent of the neutral numeric score. */
internal fun demoStatusTone(value: DemoSolutionStatus): Tone = when (value) {
    DemoSolutionStatus.Error, DemoSolutionStatus.Timeout -> Tone.Danger
    DemoSolutionStatus.Queue, DemoSolutionStatus.Checking, DemoSolutionStatus.Checked -> Tone.Info
}
