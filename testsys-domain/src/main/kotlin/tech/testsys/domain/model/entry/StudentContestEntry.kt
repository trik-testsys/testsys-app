package tech.testsys.domain.model.entry

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Identifier of a [StudentContestEntry].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class StudentContestEntryId(
    override val value: Long,
) : DomainId

/**
 * Data of a [StudentContestEntry]. Every field is fixed on creation; update is unsupported.
 *
 * @property user the entry context; fixed on creation.
 * @property studyClass the entry context; fixed on creation.
 * @property contest the entry context; fixed on creation.
 * @property enteredAt the first entry moment; fixed on creation.
 * @since %CURRENT_VERSION%
 */
data class StudentContestEntryData(
    val user: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val studyClass: LazyEntity<ClassId, Class>,
    val contest: LazyEntity<ContestId, Contest>,
    val enteredAt: Instant,
)

/**
 * The first persisted entry into its context.
 *
 * @property data the immutable entry data.
 * @since %CURRENT_VERSION%
 */
class StudentContestEntry(
    id: StudentContestEntryId,
    createdAt: Instant,
    val data: StudentContestEntryData,
) : DomainEntity<StudentContestEntryId>(id, createdAt)
