package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Identifier of a [Class].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class ClassId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Class].
 *
 * @property owner the manager who owns the class.
 * @property name the name of the class.
 * @property description the description of the class.
 * @property students the students enrolled in the class.
 * @property contests the contests assigned to the class.
 * @since %CURRENT_VERSION%
 */
data class ClassData(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val name: String,
    val description: String,
    val students: LazyEntityList<MultipleRoleUserId, MultipleRoleUser>,
    val contests: LazyEntityList<ContestId, Contest>,
)

/**
 * A study group of students that takes part in contests, owned by a manager.
 *
 * @property data the data of the class.
 * @since %CURRENT_VERSION%
 */
class Class(
    id: ClassId,
    createdAt: Instant,
    version: EntityVersion,
    val data: ClassData,
) : DomainEntity<ClassId>(id, createdAt, version)
