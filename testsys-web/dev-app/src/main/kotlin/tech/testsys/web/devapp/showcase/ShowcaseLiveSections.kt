package tech.testsys.web.devapp.showcase

import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.layout.PageScope
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

private const val HALF = 12

private const val LIVE_ROW_COUNT = 5

private const val LIVE_SUBMISSION_ID_START = 900_000L

private const val LIVE_SUBMISSION_SCORE = 87

private val LOAD_DELAY: Duration = Duration.ofSeconds(1)

private val NEW_ROW_DELAY: Duration = Duration.ofSeconds(2)

private val LOAD_SAMPLE_ROWS = listOf(
    "Тур «Весенний кубок» опубликован",
    "Участнику начислено 82 балла",
    "Открыта регистрация на «Летний марафон»",
)

private val liveSubmissionId: AtomicLong = AtomicLong(LIVE_SUBMISSION_ID_START)

/** Blocks the current thread for [duration]; simulates the latency of a real fetch in showcase loads. */
private fun pause(duration: Duration) {
    try {
        Thread.sleep(duration.toMillis())
    } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}

/** Showcase of `load()`, a table refreshed from the background and a clock following a [Signal]. */
internal fun PageScope.liveSection(clock: Signal<String>) {
    row {
        loadBlock()
        loadFailureBlock()
    }
    row {
        liveSubmissionsBlock()
        clockBlock(clock)
    }
}

private fun PageRowScope.loadBlock() {
    block(size = HALF, title = "Загрузка", subtitle = "Скелетон, пока фоновый запрос выполняется") {
        val handle = load({
            pause(LOAD_DELAY)
            LOAD_SAMPLE_ROWS
        }) { rows -> rows.forEach { line -> row { text(line) } } }
        actions { action("Загрузить снова") { onClick { handle.reload() } } }
    }
}

private fun PageRowScope.loadFailureBlock() {
    val attempts = AtomicInteger()
    block(size = HALF, title = "Ошибка загрузки", subtitle = "Первая загрузка падает, «Повторить» показывает успех") {
        load({
            pause(LOAD_DELAY)
            if (attempts.getAndIncrement() == 0) error("Showcase load failure")
            LOAD_SAMPLE_ROWS
        }) { rows -> rows.forEach { line -> row { text(line) } } }
    }
}

private fun PageRowScope.liveSubmissionsBlock() {
    val submissions: MutableList<ShowcaseSubmission> = CopyOnWriteArrayList(SUBMISSIONS.take(LIVE_ROW_COUNT))
    block(size = HALF, title = "Посылки", subtitle = "«Новая посылка» добавляет строку в фоне; новая строка подсвечивается") {
        val rows = table(
            key = { row -> row.id },
            fetch = { request -> Page(submissions.drop(request.offset).take(request.limit), submissions.size) },
        ) { submissionColumns(isCompact = true) }
        actions {
            action("Добавить посылку через 2 с") {
                onClick {
                    thread(isDaemon = true, name = "showcase-new-submission") {
                        pause(NEW_ROW_DELAY)
                        submissions.add(0, nextLiveSubmission())
                        rows.refresh()
                    }
                }
            }
        }
    }
}

private fun PageRowScope.clockBlock(clock: Signal<String>) {
    block(size = HALF, title = "Часы", subtitle = "Обновляются раз в секунду через ValueSignal") {
        row { text(clock) }
    }
}

/** A freshly "arrived" submission for the live table, with a unique id of its own range. */
private fun nextLiveSubmission(): ShowcaseSubmission = ShowcaseSubmission(
    id = liveSubmissionId.incrementAndGet(),
    author = "Новый участник",
    task = "E. Новая задача",
    verdict = ShowcaseVerdict.Accepted,
    score = LIVE_SUBMISSION_SCORE,
    sentAt = LocalDateTime.now(),
)
