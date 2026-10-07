package tech.testsys.web.devapp.demo

import com.vaadin.flow.component.UI
import com.vaadin.flow.spring.annotation.UIScope
import org.springframework.stereotype.Component
import tech.testsys.web.devapp.demo.model.DemoResult
import tech.testsys.web.devapp.demo.model.DemoSolutionStatus
import tech.testsys.web.devapp.demo.model.DemoState
import tech.testsys.web.devapp.demo.model.DemoTableState
import tech.testsys.web.devapp.demo.model.checkSolution
import tech.testsys.web.devapp.demo.model.demoFixtures
import tech.testsys.web.devapp.demo.model.finishSolution
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

private const val CHECK_START_DELAY = 500L

private const val CHECK_FINISH_DELAY = 1_500L

private const val CHECKED_SCORE = 64

/** UI-owned demonstration state; a fresh UI starts from the fixtures. */
@Component
@UIScope
internal class DemoSession {
    var state: DemoState = demoFixtures()
    val selections: MutableMap<String, String> = mutableMapOf()
    val tables: MutableMap<String, DemoTableState> = mutableMapOf()

    // Runs a task after the delay in milliseconds; tests replace it to control the time of the mock check.
    var delayedExecutor: (Long) -> Executor = { delay -> CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS) }
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
            state = state.finishSolution(id = id, status = DemoSolutionStatus.Checked, score = CHECKED_SCORE)
            render()
        }
    }

    fun reset() {
        generation++
        state = demoFixtures()
        selections.clear()
        tables.clear()
    }

    private fun schedule(ui: UI, current: Long, delay: Long, action: () -> Unit) {
        delayedExecutor(delay).execute {
            if (ui.isAttached) {
                ui.access {
                    if (current == generation) {
                        action()
                    }
                }
            }
        }
    }
}
