package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.UserId

@JvmInline
value class ClassId(
    override val value: Long
) : DomainId

data class ClassData(
    val owner: MultipleRoleUser,
    val students: LazyEntityList<UserId, MultipleRoleUser>,
    val contests: LazyEntityList<ContestId, Contest>
)

class Class(
    id: ClassId,
    val data: ClassData
) : DomainEntity<ClassId>(id)
