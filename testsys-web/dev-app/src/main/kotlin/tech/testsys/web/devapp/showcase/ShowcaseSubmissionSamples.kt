package tech.testsys.web.devapp.showcase

import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.verdict
import java.time.LocalDateTime

private const val SUBMISSION_COUNT = 140

private const val FIRST_SUBMISSION_ID = 10_001L

private const val MINUTES_BETWEEN_SUBMISSIONS = 7L

private const val SCORE_STEP = 13

internal const val SCORE_LIMIT = 101

private const val SCORE_SORT = "score"

private const val TIME_SORT = "time"

private val SHOWCASE_START: LocalDateTime = LocalDateTime.parse("2026-09-01T10:00")

private val AUTHORS = listOf("Анна Петрова", "Иван Смирнов", "Мария Козлова", "Пётр Волков", "Ольга Новикова")

internal val TASKS = listOf("A. Кратчайший путь", "B. Робот в лабиринте", "C. Движение по линии", "D. Сумма чисел")

/** Verdict of a showcase submission with its badge tone; [isError] marks a failed solution. */
internal enum class ShowcaseVerdict(val label: String, val tone: Tone, val isError: Boolean) {
    Accepted("Принято", Tone.Success, isError = false),
    WrongAnswer("Неверный ответ", Tone.Danger, isError = true),
    TimeLimit("Превышено время", Tone.Warning, isError = true),
    Running("Проверяется", Tone.Info, isError = false),
}

/** Submission row of the showcase tables; a running submission has no score yet. */
internal class ShowcaseSubmission(
    val id: Long,
    val author: String,
    val task: String,
    val verdict: ShowcaseVerdict,
    val score: Int?,
    val sentAt: LocalDateTime,
)

internal val SUBMISSIONS: List<ShowcaseSubmission> = (0 until SUBMISSION_COUNT).map { index ->
    val verdict = ShowcaseVerdict.entries[index % ShowcaseVerdict.entries.size]
    ShowcaseSubmission(
        id = FIRST_SUBMISSION_ID + index,
        author = AUTHORS[index % AUTHORS.size],
        task = TASKS[index % TASKS.size],
        verdict = verdict,
        score = if (verdict == ShowcaseVerdict.Running) null else index * SCORE_STEP % SCORE_LIMIT,
        sentAt = SHOWCASE_START.plusMinutes(index * MINUTES_BETWEEN_SUBMISSIONS),
    )
}

/** Adds the submission columns; [isCompact] gives every sized column the same width for a half-width block. */
internal fun TableScope<ShowcaseSubmission>.submissionColumns(isCompact: Boolean = false) {
    val fraction = gridColumns / SubmissionGrid.PARTS
    val wideSize = fraction * 2
    val shortSize = if (isCompact) wideSize else maxOf(SubmissionGrid.MIN_COLUMN_SIZE, fraction)
    val titleSize = if (isCompact) wideSize else fraction * SubmissionGrid.TITLE_PARTS
    codeColumn("ID", size = shortSize) { row -> "#${row.id}" }
    textColumn("Участник", size = wideSize) { row -> row.author }
    textColumn("Задача", size = titleSize) { row -> row.task }
    column("Вердикт", size = wideSize) { row -> badge(row.verdict.label, row.verdict.tone) }
    column("Баллы", sortKey = SCORE_SORT, size = shortSize) { row ->
        row.score?.let { score -> verdict(score.toDouble()) } ?: text("—")
    }
    dateTimeColumn("Время", sortKey = TIME_SORT) { row -> row.sentAt }
}

private object SubmissionGrid {
    const val MIN_COLUMN_SIZE = 2
    const val PARTS = 12
    const val TITLE_PARTS = 3
}
