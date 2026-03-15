package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaEntity

@Embeddable
data class ParticipantToCompetitionId(
    val participantId: Long,
    val competitionId: Long,
) : JpaCompositeId()

@Entity
class ParticipantToCompetitionJpaEntity(
    id: ParticipantToCompetitionId,
) : JpaCompositeEntity<ParticipantToCompetitionId>(id)

@Embeddable
data class TaskToCompetitionId(
    val taskId: Long,
    val competitionId: Long,
) : JpaCompositeId()

@Entity
class TaskToCompetitionJpaEntity(
    id: TaskToCompetitionId,
) : JpaCompositeEntity<TaskToCompetitionId>(id)

@Entity
class CompetitionJpaEntity(
    val ownerId: Long,
) : JpaEntity()