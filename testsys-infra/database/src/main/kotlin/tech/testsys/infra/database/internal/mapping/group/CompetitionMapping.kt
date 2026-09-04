package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.domain.builder.api.competition
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToCompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ParticipantToCompetitionJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Competition] and [CompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object CompetitionMapping : EntityMapping<Competition, CompetitionJpaEntity> {

    /**
     * Assembles a [Competition] from [jpaEntity] and the ids of its participants and contests.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: CompetitionJpaEntity, participantIds: List<SingleRoleUserId>, contestIds: List<ContestId>) = competition {
        populateFields(jpaEntity)
        data {
            owner(jpaEntity.ownerId)

            name = jpaEntity.name
            description = jpaEntity.description
            participants = participantIds.toMutableList()
            contests = contestIds.toMutableList()
        }
    }

    /**
     * Creates a new [CompetitionJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: CompetitionData) = CompetitionJpaEntity(
        name = data.name,
        description = data.description,
        ownerId = data.owner.id.value,
    )

    /**
     * Creates the [CompetitionJpaEntity] row replacing [current] from [entity], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Competition, current: CompetitionJpaEntity) = CompetitionJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        ownerId = entity.data.owner.id.value,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.version.value
    }

    /**
     * Creates the [ParticipantToCompetitionJpaEntity] rows linking the competition [competitionId] with [participantIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toParticipantAssociations(competitionId: Long, participantIds: List<SingleRoleUserId>) = participantIds.map {
        ParticipantToCompetitionJpaEntity(
            participantId = it.value,
            competitionId = competitionId,
        )
    }

    /**
     * Creates the [ContestToCompetitionJpaEntity] rows linking the competition [competitionId] with [contestIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toContestAssociations(competitionId: Long, contestIds: List<ContestId>) = contestIds.map {
        ContestToCompetitionJpaEntity(
            contestId = it.value,
            competitionId = competitionId,
        )
    }
}
