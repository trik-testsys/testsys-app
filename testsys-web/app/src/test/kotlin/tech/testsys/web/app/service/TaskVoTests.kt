package tech.testsys.web.app.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.task
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TestId
import java.time.Instant

class TaskVoTests {
    @Test
    fun `should map a new task to a working revision without a committed one`() {
        val vo = testTask { new { tests(listOf(1)) } }.toVo()

        assertEquals(listOf(TestId(1)), vo.wip?.tests)
        assertNull(vo.wip?.statement)
        assertNull(vo.lastCommitted)
    }

    @Test
    fun `should map an uncommitted task to both revisions`() {
        val vo = testTask {
            uncommitted(
                wipBuilder = { tests(listOf(2)) },
                lastCommittedBuilder = {
                    exercises(listOf(3))
                    statement(4)
                },
            )
        }.toVo()

        assertEquals(listOf(TestId(2)), vo.wip?.tests)
        assertEquals(listOf(ExerciseId(3)), vo.lastCommitted?.exercises)
        assertEquals(StatementId(4), vo.lastCommitted?.statement)
    }

    @Test
    fun `should map a committed task to a committed revision without a working one`() {
        val vo = testTask {
            committed {
                exercises(listOf(3))
                statement(4)
            }
        }.toVo()

        assertNull(vo.wip)
        assertEquals(StatementId(4), vo.lastCommitted?.statement)
    }

    private fun testTask(choose: TaskContentChooser.() -> Unit): Task = task {
        id = 1
        createdAt = Instant.EPOCH
        data = taskData {
            owner(1)
            name = "name"
            description = "description"
            content.choose()
        }
    }
}
