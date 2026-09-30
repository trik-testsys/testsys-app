package tech.testsys.web.dev

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.ui.TestSysView
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.data.LeaderboardCell
import tech.testsys.web.ui.data.LeaderboardCellState
import tech.testsys.web.ui.data.LeaderboardColumn
import tech.testsys.web.ui.data.LeaderboardData
import tech.testsys.web.ui.data.LeaderboardRow
import tech.testsys.web.ui.data.leaderboard
import tech.testsys.web.ui.data.sortableList
import tech.testsys.web.ui.display.AvatarData
import tech.testsys.web.ui.display.ContestCardData
import tech.testsys.web.ui.display.DifficultyLevel
import tech.testsys.web.ui.display.ProgressValue
import tech.testsys.web.ui.display.TimerValue
import tech.testsys.web.ui.display.TimerVariant
import tech.testsys.web.ui.display.Tone
import tech.testsys.web.ui.display.avatar
import tech.testsys.web.ui.display.avatarGroup
import tech.testsys.web.ui.display.contestCard
import tech.testsys.web.ui.display.difficulty
import tech.testsys.web.ui.display.progressBar
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.display.timer
import tech.testsys.web.ui.feedback.SkeletonShape
import tech.testsys.web.ui.feedback.skeleton
import tech.testsys.web.ui.feedback.skeletonRows
import tech.testsys.web.ui.layout.PageScope
import tech.testsys.web.ui.navigation.StepData
import tech.testsys.web.ui.navigation.StepperData
import tech.testsys.web.ui.navigation.stepper
import tech.testsys.web.ui.quiz.QuestionNavData
import tech.testsys.web.ui.quiz.QuizOptionData
import tech.testsys.web.ui.quiz.QuizResult
import tech.testsys.web.ui.quiz.questionNav
import tech.testsys.web.ui.quiz.quizOption
import java.time.Duration
import java.time.Instant

private const val HALF_WIDTH = 12
private const val FULL_WIDTH = 24
private const val DEMO_PROGRESS_PERCENT = 65.0
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
            }
            block(title = "Skeleton") {
                row { horizontal { SkeletonShape.entries.forEach { shape -> skeleton(shape) } } }
                row { vertical { skeletonRows(SKELETON_ROW_COUNT) } }
            }
            contestCards()
            block(title = "Leaderboard") { leaderboard(demoLeaderboard()) }
            block(title = "SortableList", subtitle = "Мышь: перетащите ручку. Клавиатура: Space, стрелки, Enter; Escape отменяет") {
                row {
                    vertical {
                        sortableList(
                            items = listOf("Первый", "Второй", "Третий"),
                            itemKey = { value -> value },
                            itemLabel = { value -> value },
                            content = { value -> text(value) },
                        )
                    }
                }
            }
            block(title = "Stepper, QuizOption, QuestionNav") {
                row {
                    val steps = listOf(
                        StepData("Начало"),
                        StepData("Вопросы"),
                        StepData("Результат", isSelectable = false),
                    )
                    stepper(StepperData(steps, current = 1))
                }
                row {
                    vertical {
                        quizOption(QuizOptionData(label = "Обычный ответ", letter = "A"))
                        quizOption(QuizOptionData(label = "Несколько ответов", letter = "B", isSelected = true, isMultiple = true))
                        quizOption(QuizOptionData(label = "Верный ответ", letter = "C", result = QuizResult.Correct))
                        quizOption(QuizOptionData(label = "Неверный ответ", letter = "D", result = QuizResult.Wrong))
                        val navigation = QuestionNavData(
                            total = QUESTION_COUNT,
                            current = CURRENT_QUESTION,
                            answered = setOf(1, 2),
                            flagged = setOf(FLAGGED_QUESTION),
                            unavailable = setOf(QUESTION_COUNT),
                        )
                        questionNav(navigation)
                    }
                }
            }
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
                )
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
    columns = listOf(
        LeaderboardColumn(key = "a", label = "A"),
        LeaderboardColumn(key = "b", label = "B"),
        LeaderboardColumn(key = "total", label = "Итого", isMetric = true),
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
