package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
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
class StudentToClassJpaEntity(
    id: StudentToClassId,
) : CompositeJpaEntity<StudentToClassId>(id) {

    constructor(studentId: Long, classId: Long): this(StudentToClassId(studentId, classId))
}

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
) : CompositeJpaEntity<ContestToClassId>(id) {

    constructor(contestId: Long, classId: Long): this(ContestToClassId(contestId, classId))
}

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
    val description: String,
    val ownerId: Long,
) : SequenceJpaEntity()
