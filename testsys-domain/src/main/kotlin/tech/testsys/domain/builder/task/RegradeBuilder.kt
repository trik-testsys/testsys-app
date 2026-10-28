package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.Regrade
import tech.testsys.domain.model.task.RegradeData
import tech.testsys.domain.model.task.RegradeId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId

/**
 * Builder of [RegradeData]. Required: [task].
 *
 * @property task the id of the regraded task, or `null` if not set yet.
 * @property tests the ids of the polygons of the committed revision.
 * @property submissions the ids of the submissions sent to grading again.
 * @since %CURRENT_VERSION%
 */
class RegradeDataBuilder : Builder<RegradeData> {

    var task: TaskId? = null

    var tests = mutableListOf<TestId>()

    var submissions = mutableListOf<SubmissionId>()

    /**
     * Sets [task] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun task(task: Long) {
        this.task = TaskId(task)
    }

    /**
     * Sets [tests] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun tests(tests: Iterable<Long>) {
        this.tests = tests.map { TestId(it) }.toMutableList()
    }

    /**
     * Sets [submissions] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun submissions(submissions: Iterable<Long>) {
        this.submissions = submissions.map { SubmissionId(it) }.toMutableList()
    }

    override fun build(): RegradeData {
        val task = requireField(task) { ::task }

        return RegradeData(
            task = task.lazify(),
            tests = tests.lazify(),
            submissions = submissions.lazify(),
        )
    }
}

/**
 * Builder of [Regrade] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class RegradeBuilder : DomainEntityWithDataBuilder<Regrade, RegradeData, RegradeDataBuilder>() {

    override fun dataBuilder() = RegradeDataBuilder()

    override fun build(): Regrade {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Regrade(
            id = RegradeId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
