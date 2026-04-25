package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

@Embeddable
data class ClassToStudentId(
    val classId: Long,
    val studentId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class ClassToStudentJpaEntity(
    id: ClassToStudentId,
) : CompositeJpaEntity<ClassToStudentId>(id) {

    constructor(classId: Long, studentId: Long): this(ClassToStudentId(classId, studentId))
}

@Entity
class StudentDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
