package tech.testsys.domain.contract.persistence

import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.UserId

/**
 * Optional task selection criteria, combined with AND after access is checked.
 *
 * @property name a literal case-insensitive substring, preserving spaces; an empty string matches every name.
 * @property ownerId the required owner, or `null` for any authorized owner.
 * @property state the required lifecycle state, or `null` for any state.
 * @since %CURRENT_VERSION%
 */
data class TaskFilter(
    val name: String? = null,
    val ownerId: MultipleRoleUserId? = null,
    val state: State? = null,
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
 * @since %CURRENT_VERSION%
 */
data class ContestFilter(
    val name: String? = null,
    val ownerId: MultipleRoleUserId? = null,
)

/**
 * Optional verdict selection criteria applied to verdicts available to a judge.
 *
 * @property authorId the required submission author, or `null` for every eligible author.
 * @since %CURRENT_VERSION%
 */
data class VerdictFilter(val authorId: UserId? = null)
