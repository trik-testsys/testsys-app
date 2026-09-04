package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.StudentToClassJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Mapping between [Class] and [ClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object ClassMapping : EntityMapping<Class, ClassJpaEntity> {

    /**
     * Assembles a [Class] from [jpaEntity] and the ids of its students and contests.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: ClassJpaEntity, studentIds: List<MultipleRoleUserId>, contestIds: List<ContestId>) = `class` {
        id = jpaEntity.requireId()
        createdAt = jpaEntity.createdAt
        data {
            owner(jpaEntity.ownerId)

            name = jpaEntity.name
            description = jpaEntity.description
            students = studentIds.toMutableList()
            contests = contestIds.toMutableList()
        }
    }

    /**
     * Creates a new [ClassJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: ClassData) = ClassJpaEntity(
        name = data.name,
        description = data.description,
        ownerId = data.owner.id.value,
    )

    /**
     * Creates the [ClassJpaEntity] row replacing [current] from [entity], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Class, current: ClassJpaEntity) = ClassJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        ownerId = entity.data.owner.id.value,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }

    /**
     * Creates the [StudentToClassJpaEntity] rows linking the class [classId] with [studentIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toStudentAssociations(classId: Long, studentIds: List<MultipleRoleUserId>) = studentIds.map {
        StudentToClassJpaEntity(
            studentId = it.value,
            classId = classId,
        )
    }

    /**
     * Creates the [ContestToClassJpaEntity] rows linking the class [classId] with [contestIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toContestAssociations(classId: Long, contestIds: List<ContestId>) = contestIds.map {
        ContestToClassJpaEntity(
            contestId = it.value,
            classId = classId,
        )
    }
}
