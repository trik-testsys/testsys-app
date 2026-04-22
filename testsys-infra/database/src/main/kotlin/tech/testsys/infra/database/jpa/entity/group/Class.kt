package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.DescribableJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId

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
class StudentToClassJpaEntity(
    id: StudentToClassId,
) : CompositeJpaEntity<StudentToClassId>(id)

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
class ContestToClassJpaEntity(
    id: ContestToClassId,
) : CompositeJpaEntity<ContestToClassId>(id)

/**
 * JPA entity representing a class domain entity.
 *
 * @see tech.testsys.domain.model.group.Class
 * @see tech.testsys.domain.model.group.ClassData
 * @since %CURRENT_VERSION%
 */
@Entity
class ClassJpaEntity(
    name: String,
    description: String,
    val ownerId: Long,
) : DescribableJpaEntity(name, description)
