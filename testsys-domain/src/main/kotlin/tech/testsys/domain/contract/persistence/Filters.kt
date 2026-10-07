package tech.testsys.domain.contract.persistence

import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.UserId
import java.time.Instant

/**
 * Optional task selection criteria, combined with AND after access is checked.
 *
 * @property name a literal case-insensitive substring, preserving spaces; an empty string matches every name.
 * @property ownerId the required owner, or `null` for any authorized owner.
 * @property state the required lifecycle state, or `null` for any state.
 * @property communityId the community granted access to the task, or null for any sharing; never expands access.
 * @since %CURRENT_VERSION%
 */
data class TaskFilter(
    val name: String? = null,
    val ownerId: MultipleRoleUserId? = null,
    val state: State? = null,
    val communityId: CommunityId? = null,
) {

    /**
     * Lifecycle state to select.
     *
     * @since %CURRENT_VERSION%
     */
    enum class State {
        NEW,
        UNCOMMITTED,
        COMMITTED,
    }
}

/**
 * Optional contest selection criteria, combined with AND after access is checked.
 *
 * @property name a literal case-insensitive substring, preserving spaces; an empty string matches every name.
 * @property ownerId the required owner, or `null` for any authorized owner.
 * @property communityId the community granted access to the contest, or null for any sharing; never expands access.
 * @since %CURRENT_VERSION%
 */
data class ContestFilter(
    val name: String? = null,
    val ownerId: MultipleRoleUserId? = null,
    val communityId: CommunityId? = null,
)

/**
 * Optional verdict selection criteria, combined with AND before paging and counting authorized verdicts.
 *
 * @property authorId the required submission author, or `null` for every eligible author.
 * @property submissionId the submission whose current successful verdict is selected, or null for any submission.
 * @property classId the class containing both the current student author and the contest, or null for any class.
 * @property competitionId the competition containing both the current participant author and the contest, or null for any competition.
 * @since %CURRENT_VERSION%
 */
data class VerdictFilter(
    val authorId: UserId? = null,
    val submissionId: SubmissionId? = null,
    val classId: ClassId? = null,
    val competitionId: CompetitionId? = null,
)

/**
 * Optional class selection criteria, combined with AND before paging and counting owned classes.
 *
 * @property name a literal case-insensitive substring, preserving spaces; an empty string matches every name.
 * @property createdFrom the inclusive lower creation-time bound, or null for no lower bound.
 * @property createdTo the inclusive upper creation-time bound, or null for no upper bound.
 * @throws IllegalArgumentException if [createdFrom] is later than [createdTo].
 * @since %CURRENT_VERSION%
 */
data class ClassFilter(
    val name: String? = null,
    val createdFrom: Instant? = null,
    val createdTo: Instant? = null,
) {

    init {
        require(createdFrom == null || createdTo == null || createdFrom <= createdTo) {
            "Class creation lower bound $createdFrom must not be later than upper bound $createdTo"
        }
    }
}
