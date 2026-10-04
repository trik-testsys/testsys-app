package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Builder of [JudgmentOrderData]. Required: [judge], [submission], [score], [reason].
 *
 * @property judge the id of the issuing judge, or `null` if not set yet.
 * @property submission the id of the submission the order applies to, or `null` if not set yet.
 * @property score the score awarded by the judge, or `null` if not set yet.
 * @property reason the justification of the ruling, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class JudgmentOrderDataBuilder : Builder<JudgmentOrderData> {

    var judge: MultipleRoleUserId? = null

    var submission: SubmissionId? = null

    var score: Int? = null

    var reason: String? = null

    /**
     * Sets [judge] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun judge(id: Long) {
        this.judge = MultipleRoleUserId(id)
    }

    /**
     * Sets [submission] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun submission(id: Long) {
        this.submission = SubmissionId(id)
    }

    override fun build(): JudgmentOrderData {
        val judge = requireField(judge) { ::judge }
        val submission = requireField(submission) { ::submission }
        val score = requireField(score) { ::score }
        val reason = requireField(reason) { ::reason }

        return JudgmentOrderData(
            judge = judge.lazify(),
            submission = submission.lazify(),
            score = Score(score),
            reason = reason,
        )
    }
}

/**
 * Builder of [JudgmentOrder] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class JudgmentOrderBuilder : DomainEntityWithDataBuilder<JudgmentOrder, JudgmentOrderData, JudgmentOrderDataBuilder>() {

    override fun dataBuilder() = JudgmentOrderDataBuilder()

    override fun build(): JudgmentOrder {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return JudgmentOrder(
            id = JudgmentOrderId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
