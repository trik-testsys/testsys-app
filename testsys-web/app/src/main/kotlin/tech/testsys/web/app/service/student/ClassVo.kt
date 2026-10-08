package tech.testsys.web.app.service.student

import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Class data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the class.
 * @property createdAt the moment the class was created.
 * @property owner the identifier of the owner.
 * @property name the name of the class.
 * @property description the description of the class.
 * @property students the identifiers of the students.
 * @property contests the identifiers of the contests of the class.
 * @since %CURRENT_VERSION%
 */
data class ClassVo(
    val id: ClassId,
    val createdAt: Instant,
    val owner: MultipleRoleUserId,
    val name: String,
    val description: String,
    val students: List<MultipleRoleUserId>,
    val contests: List<ContestId>,
)

internal fun Class.toVo(): ClassVo = ClassVo(
    id = id,
    createdAt = createdAt,
    owner = data.owner.id,
    name = data.name,
    description = data.description,
    students = data.students.ids,
    contests = data.contests.ids,
)
