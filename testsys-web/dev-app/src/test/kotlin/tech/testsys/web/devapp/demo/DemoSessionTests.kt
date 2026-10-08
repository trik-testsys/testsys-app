package tech.testsys.web.devapp.demo

import com.github.mvysny.kaributesting.v10.MockVaadin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.testsys.web.devapp.demo.model.DemoSolutionKind
import tech.testsys.web.devapp.demo.model.DemoSolutionStatus
import tech.testsys.web.devapp.demo.model.DemoState
import tech.testsys.web.devapp.demo.model.submitSolution
import java.util.concurrent.Executor

class DemoSessionTests {
    private val session = DemoSession()
    private val scheduled = mutableListOf<Pair<Long, Runnable>>()
    private var renders = 0

    @BeforeEach
    fun setUp() {
        MockVaadin.setup()
        session.delayedExecutor = { delay -> Executor { task -> scheduled += delay to task } }
        session.state = queued(session.state)
    }

    @AfterEach
    fun tearDown() = MockVaadin.tearDown()

    @Test
    fun `should schedule the check start and finish after their delays`() {
        session.check(SOLUTION) { renders++ }

        assertEquals(listOf(500L, 1_500L), scheduled.map { (delay, _) -> delay })
    }

    @Test
    fun `should move a queued solution to checking after the first delay`() {
        session.check(SOLUTION) { renders++ }

        runScheduled(0)

        assertEquals(DemoSolutionStatus.Checking, solution().status)
        assertEquals(1, renders)
    }

    @Test
    fun `should finish the check with a score after the second delay`() {
        session.check(SOLUTION) { renders++ }
        runScheduled(0)

        runScheduled(1)

        assertEquals(DemoSolutionStatus.Checked, solution().status)
        assertEquals(64, solution().score)
    }

    @Test
    fun `should ignore pending check steps after a reset`() {
        session.check(SOLUTION) { renders++ }
        session.reset()
        session.state = queued(session.state)

        runScheduled(0)

        assertEquals(DemoSolutionStatus.Queue, solution().status)
        assertEquals(0, renders)
    }

    private fun runScheduled(index: Int) {
        scheduled[index].second.run()
        MockVaadin.clientRoundtrip()
    }

    private fun solution() = session.state.solutions.single { solution -> solution.id == SOLUTION }

    private fun queued(state: DemoState): DemoState =
        state.submitSolution(userId = "student", taskId = "task1", kind = DemoSolutionKind.Python, fileName = "line.py").state

    private companion object {
        const val SOLUTION = "s103"
    }
}
