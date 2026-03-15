package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaEntity

@Embeddable
data class StudentToClassId(
    val studentId: Long,
    val classId: Long,
) : JpaCompositeId()

@Entity
class StudentToClassJpaEntity(
    id: StudentToClassId,
) : JpaCompositeEntity<StudentToClassId>(id)

@Entity
class ClassJpaEntity(
    val ownerId: Long,
) : JpaEntity()
