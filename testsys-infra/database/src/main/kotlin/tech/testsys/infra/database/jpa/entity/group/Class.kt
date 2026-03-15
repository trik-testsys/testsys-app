package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * Composite primary key for [StudentToClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class StudentToClassId(
    val studentId: Long,
    val classId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing a student to class association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class StudentToClassJpaEntity(
    id: StudentToClassId,
) : JpaCompositeEntity<StudentToClassId>(id)

/**
 * Composite primary key for [ManagerToClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class ManagerToClassId(
    val managerId: Long,
    val classId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing a manager to class association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class ManagerToClassJpaEntity(
    id: ManagerToClassId,
) : JpaCompositeEntity<ManagerToClassId>(id)

/**
 * Composite primary key for [ContestToClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class ContestToClassId(
    val contestId: Long,
    val classId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing a contest to class association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class ContestToClassJpaEntity(
    id: ContestToClassId,
) : JpaCompositeEntity<ContestToClassId>(id)

/**
 * JPA entity representing a class domain entity.
 *
 * @see tech.testsys.domain.model.group.Class
 * @see tech.testsys.domain.model.group.ClassData
 * @since %CURRENT_VERSION%
 */
@Entity
class ClassJpaEntity(
    val ownerId: Long,
) : JpaEntity()
