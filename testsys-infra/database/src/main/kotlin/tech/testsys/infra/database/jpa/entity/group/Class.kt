package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * Composite primary key for [StudentToClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class StudentToClassId(
    val studentId: Long,
    val classId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity representing a student to class association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
class StudentToClassJpaEntity(id: StudentToClassId) : CompositeJpaEntity<StudentToClassId>(id)

/**
 * Composite primary key for [ContestToClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class ContestToClassId(
    val contestId: Long,
    val classId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity representing a contest to class association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
class ContestToClassJpaEntity(id: ContestToClassId) : CompositeJpaEntity<ContestToClassId>(id)

/**
 * JPA entity representing a class domain entity.
 *
 * @see tech.testsys.domain.model.group.Class
 * @see tech.testsys.domain.model.group.ClassData
 * @since %CURRENT_VERSION%
 */
@Entity
class ClassJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val ownerId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)
