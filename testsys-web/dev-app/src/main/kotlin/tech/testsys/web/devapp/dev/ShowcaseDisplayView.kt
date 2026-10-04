package tech.testsys.web.devapp.dev

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.LeaderboardCell
import tech.testsys.web.components.data.LeaderboardCellState
import tech.testsys.web.components.data.LeaderboardColumn
import tech.testsys.web.components.data.LeaderboardData
import tech.testsys.web.components.data.LeaderboardRow
import tech.testsys.web.components.data.SortableListHandle
import tech.testsys.web.components.data.leaderboard
import tech.testsys.web.components.data.sortableList
import tech.testsys.web.components.display.AvatarData
import tech.testsys.web.components.display.ContestCardData
import tech.testsys.web.components.display.DifficultyLevel
import tech.testsys.web.components.display.LegacyVerdict
import tech.testsys.web.components.display.ProgressValue
import tech.testsys.web.components.display.TimerValue
import tech.testsys.web.components.display.TimerVariant
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.avatar
import tech.testsys.web.components.display.avatarGroup
import tech.testsys.web.components.display.contestCard
import tech.testsys.web.components.display.difficulty
import tech.testsys.web.components.display.legacyVerdict
import tech.testsys.web.components.display.progressBar
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.timer
import tech.testsys.web.components.display.verdict
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.SkeletonShape
import tech.testsys.web.components.feedback.skeleton
import tech.testsys.web.components.feedback.skeletonRows
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.StepData
import tech.testsys.web.components.navigation.StepperData
import tech.testsys.web.components.navigation.stepper
import tech.testsys.web.components.quiz.QuestionNavData
import tech.testsys.web.components.quiz.QuizOptionData
import tech.testsys.web.components.quiz.QuizResult
import tech.testsys.web.components.quiz.questionNav
import tech.testsys.web.components.quiz.quizOption
import java.time.Duration
import java.time.Instant

private const val HALF_WIDTH = 12
private const val FULL_WIDTH = 24
private const val DEMO_PROGRESS_PERCENT = 65.0
private const val COMPLETE_PROGRESS_PERCENT = 100.0
private const val LIVE_TIMER_SECONDS = 65L
private const val STATIC_TIMER_SECONDS = 3725L
private const val SKELETON_ROW_COUNT = 3
private const val QUESTION_COUNT = 10
private const val CURRENT_QUESTION = 3
private const val FLAGGED_QUESTION = 5

/**
 * Showcase of displays, ordering and questions without contest business calculations, available only in dev.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/display")
@PageTitle("Отображение и данные")
class ShowcaseDisplayView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Отображение и данные")
            block(title = "Avatar, AvatarGroup, Difficulty") {
                row {
                    horizontal {
                        avatar(AvatarData("Анна Петрова"))
                        avatar(AvatarData("TRIK", isSquare = true))
                        val names = listOf("Анна Петрова", "Иван Иванов", "Мария Соколова", "Пётр Сергеев")
                        avatarGroup(names.map { name -> AvatarData(name) })
                        DifficultyLevel.entries.forEach { level -> difficulty(level) }
                        difficulty(DifficultyLevel.Easy, showLabel = false)
                    }
                }
            }
            block(title = "ProgressBar и Timer") {
                row {
                    progressBar("Прогресс", ProgressValue.Determinate(DEMO_PROGRESS_PERCENT), size = HALF_WIDTH)
                    progressBar("Ожидание", ProgressValue.Indeterminate, size = HALF_WIDTH)
                }
                TimerVariant.entries.forEach { variant ->
                    row { timer("Осталось времени", TimerValue.Static(Duration.ofSeconds(STATIC_TIMER_SECONDS)), variant = variant) }
                }
                row { timer("Живой таймер", TimerValue.Until(Instant.now().plusSeconds(LIVE_TIMER_SECONDS))) }
                row { timer("Завершённый таймер", TimerValue.Static(Duration.ZERO)) }
                row {
                    progressBar("Начало", ProgressValue.Determinate(0.0), size = HALF_WIDTH)
                    progressBar("Готово", ProgressValue.Determinate(COMPLETE_PROGRESS_PERCENT), size = HALF_WIDTH, tone = Tone.Success)
                }
                row {
                    horizontal {
                        listOf(Tone.Info, Tone.Success, Tone.Warning, Tone.Danger).forEach { tone ->
                            progressBar("Прогресс ${tone.name}", ProgressValue.Determinate(DEMO_PROGRESS_PERCENT), tone = tone)
                        }
                    }
                }
            }
            block(title = "Skeleton") {
                row { horizontal { SkeletonShape.entries.forEach { shape -> skeleton(shape) } } }
                row { vertical { skeletonRows(SKELETON_ROW_COUNT) } }
            }
            contestCards()
            block(title = "Leaderboard") { leaderboard(demoLeaderboard()) }
            numericVerdicts()
            orderingExamples()
            questionExamples()
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }
}

private fun PageScope.contestCards() {
    row {
        slot(FULL_WIDTH) {
            row {
                contestCard(
                    ContestCardData(
                        title = "Открытая практика",
                        format = "Практика",
                        status = "Открыта",
                        tone = Tone.Info,
                        whenText = "В любое время",
                        tags = listOf("TRIK", "Python"),
                        people = "24 участника",
                        actionLabel = "Участвовать",
                    ),
                    size = HALF_WIDTH,
                ) {
                    onClick { toast(FeedbackKind.Info, "Открыта карточка практики") }
                    onAction { toast(FeedbackKind.Success, "Выбрано участие в практике") }
                }
                contestCard(
                    ContestCardData(
                        title = "Завершённая тренировка",
                        format = "Тренировка",
                        status = "Завершена",
                        tone = Tone.Neutral,
                        whenText = "Вчера",
                    ),
                    size = HALF_WIDTH,
                )
            }
        }
    }
}

private fun demoLeaderboard(): LeaderboardData = LeaderboardData(
    label = "Таблица результатов",
    placeLabel = "Место",
    identityLabel = "Участник",
    placeSize = 2,
    identitySize = 8,
    columns = listOf(
        LeaderboardColumn(key = "a", label = "A", size = 4),
        LeaderboardColumn(key = "b", label = "B", size = 4),
        LeaderboardColumn(key = "total", label = "Итого", isMetric = true, size = null),
    ),
    rows = listOf(
        LeaderboardRow(
            key = "first",
            place = "1",
            name = "Анна Петрова",
            description = "Команда TRIK",
            cells = mapOf(
                "a" to LeaderboardCell(value = "100", detail = "02:10", state = LeaderboardCellState.HighlightedSuccess),
                "b" to LeaderboardCell("80", state = LeaderboardCellState.Success),
                "total" to LeaderboardCell("180"),
            ),
            isHighlighted = true,
        ),
        LeaderboardRow(
            key = "second",
            place = "2",
            name = "Иван Иванов",
            cells = mapOf(
                "a" to LeaderboardCell("60", state = LeaderboardCellState.Error),
                "b" to LeaderboardCell("…", state = LeaderboardCellState.Pending),
                "total" to LeaderboardCell("60"),
            ),
        ),
    ),
)

private fun PageScope.numericVerdicts() {
    block(title = "Verdict: числовой результат") {
        row {
            horizontal {
                verdict(0.0)
                verdict(DEMO_PROGRESS_PERCENT, label = "баллов")
                text("Число не задаёт порог успешности; очередь, ошибка и тайм-аут — отдельные статусы")
            }
        }
        row { verdict(0.0, label = "баллов", size = HALF_WIDTH) }
    }
    block(title = "Legacy Verdict: совместимость", subtitle = "Коды олимпиадного программирования; типизированный пример") {
        row {
            horizontal {
                LegacyVerdict.entries.forEach { value -> legacyVerdict(value) }
            }
        }
    }
}

private fun PageScope.orderingExamples() {
    val initial = listOf("Первый", "Второй", "Третий")
    lateinit var result: TextHandle
    lateinit var order: SortableListHandle<String>
    block(title = "SortableList", subtitle = "Мышь: ручка; клавиатура: Space, стрелки, Enter; Escape отменяет") {
        row {
            vertical {
                order = sortableList(
                    items = initial,
                    itemKey = { value -> value },
                    itemLabel = { value -> value },
                    content = { value -> text(value) },
                ) {
                    onChange { values -> result.text = "Порядок: ${values.joinToString(" → ")}" }
                }
            }
        }
        row { result = text("Порядок: ${initial.joinToString(" → ")}") }
        actions {
            action("Сбросить порядок") {
                onClick {
                    order.items = initial
                    result.text = "Порядок: ${initial.joinToString(" → ")}"
                }
            }
            action("Переключить доступность порядка") { onClick { order.isEnabled = !order.isEnabled } }
        }
    }
}

private fun PageScope.questionExamples() {
    lateinit var stepResult: TextHandle
    lateinit var answerResult: TextHandle
    lateinit var questionResult: TextHandle
    lateinit var steps: SelectionHandle<StepperData>
    lateinit var option: SelectionHandle<QuizOptionData>
    lateinit var questions: SelectionHandle<QuestionNavData>
    val stepData = StepperData(
        steps = listOf(StepData("Начало"), StepData("Вопросы"), StepData("Результат", isSelectable = false)),
        current = 1,
    )

    val navigation = QuestionNavData(
        total = QUESTION_COUNT,
        current = CURRENT_QUESTION,
        answered = setOf(1, 2),
        flagged = setOf(FLAGGED_QUESTION),
        unavailable = setOf(QUESTION_COUNT),
    )
    block(title = "Stepper, QuizOption, QuestionNav") {
        row { steps = stepper(stepData) { onChange { value -> stepResult.text = "Выбран шаг: ${value.current + 1}" } } }
        row { stepResult = text("Выбран шаг: 2") }
        row {
            vertical {
                option = quizOption(QuizOptionData(label = "Обычный ответ", letter = "A")) {
                    onChange { value -> answerResult.text = "Ответ выбран: ${value.isSelected}" }
                }
                quizOption(QuizOptionData(label = "Несколько ответов", letter = "B", isSelected = true, isMultiple = true)) {
                    onChange { value -> answerResult.text = "Несколько ответов: ${value.isSelected}" }
                }
                quizOption(QuizOptionData(label = "Верный ответ", letter = "C", result = QuizResult.Correct))
                quizOption(QuizOptionData(label = "Неверный ответ", letter = "D", result = QuizResult.Wrong))
                questions = questionNav(navigation) { onChange { value -> questionResult.text = "Текущий вопрос: ${value.current}" } }
            }
        }
        row { answerResult = text("Ответ выбран: false") }
        row { questionResult = text("Текущий вопрос: $CURRENT_QUESTION") }
        actions {
            action("Сбросить выборы") {
                onClick {
                    steps.data = stepData
                    option.data = option.data.copy(isSelected = false)
                    questions.data = navigation
                    stepResult.text = "Выбран шаг: 2"
                    answerResult.text = "Ответ выбран: false"
                    questionResult.text = "Текущий вопрос: $CURRENT_QUESTION"
                }
            }
        }
    }
}
