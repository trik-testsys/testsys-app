package tech.testsys.web.devapp.showcase

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.display.ProgressValue
import tech.testsys.web.components.display.TimerValue
import tech.testsys.web.components.display.TimerVariant
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.progressBar
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.timer
import tech.testsys.web.components.display.verdict
import tech.testsys.web.components.feedback.SkeletonShape
import tech.testsys.web.components.feedback.skeleton
import tech.testsys.web.components.feedback.skeletonRows
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.texts.UiTexts
import java.time.Duration
import java.time.Instant

private const val HALF_WIDTH = 12
private const val DEMO_PROGRESS_PERCENT = 65.0
private const val COMPLETE_PROGRESS_PERCENT = 100.0
private const val LIVE_TIMER_SECONDS = 65L
private const val STATIC_TIMER_SECONDS = 3725L
private const val SKELETON_ROW_COUNT = 3

/**
 * Showcase of displays without contest business calculations, available only in dev.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/display")
@PageTitle("Отображение и данные")
class ShowcaseDisplayView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Отображение и данные")
            row {
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
            }
            row {
                block(title = "Skeleton") {
                    row { horizontal { SkeletonShape.entries.forEach { shape -> skeleton(shape) } } }
                    row { vertical { skeletonRows(SKELETON_ROW_COUNT) } }
                }
            }
            numericVerdicts()
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }
}

private fun PageScope.numericVerdicts() {
    row {
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
    }
}
