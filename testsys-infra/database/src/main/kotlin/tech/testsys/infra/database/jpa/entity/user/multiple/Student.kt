package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * Composite primary key for [ClassToStudentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class ClassToStudentId(
    val classId: Long,
    val studentId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity associating a class with a student (a student may belong to multiple classes).
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class ClassToStudentJpaEntity(
    id: ClassToStudentId,
) : CompositeJpaEntity<ClassToStudentId>(id) {

    constructor(classId: Long, studentId: Long): this(ClassToStudentId(classId, studentId))
}

/**
 * JPA entity representing the student role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.StudentData
 * @since %CURRENT_VERSION%
 */
@Entity
class StudentDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
