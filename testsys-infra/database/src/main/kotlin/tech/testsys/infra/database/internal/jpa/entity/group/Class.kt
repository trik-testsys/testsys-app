package tech.testsys.infra.database.internal.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * Composite key of [StudentToClassJpaEntity].
 *
 * @property studentId id of the student.
 * @property classId id of the class.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class StudentToClassId(
    val studentId: Long,
    val classId: Long,
) : CompositeId

/**
 * Join row: a student is a member of a class.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class StudentToClassJpaEntity(id: StudentToClassId) : CompositeJpaEntity<StudentToClassId>(id)

/**
 * Composite key of [ContestToClassJpaEntity].
 *
 * @property contestId id of the contest.
 * @property classId id of the class.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class ContestToClassId(
    val contestId: Long,
    val classId: Long,
) : CompositeId

/**
 * Join row: a contest is assigned to a class.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class ContestToClassJpaEntity(id: ContestToClassId) : CompositeJpaEntity<ContestToClassId>(id)

/**
 * JPA entity of [tech.testsys.domain.model.group.Class].
 *
 * @property name the name of the class.
 * @property description the description of the class.
 * @property ownerId id of the manager owning the class.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ClassJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val ownerId: Long,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
