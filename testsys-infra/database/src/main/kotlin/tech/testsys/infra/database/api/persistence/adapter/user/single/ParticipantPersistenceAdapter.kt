package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.user.AbstractUserPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.ParticipantDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.UserSingleRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.group.CompetitionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ParticipantDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.single.ParticipantMapping
import tech.testsys.infra.database.internal.utils.findAllInChunks
import tech.testsys.infra.database.internal.utils.requireById
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Persistence adapter of [Participant] entities backed by [UserJpaEntity] rows having a participant data row.
 * The competition is fixed on creation and ignored on update. Update and remove increment the user version first;
 * [save], [saveToCompetition] and remove also increment the competition version.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ParticipantPersistenceAdapter(
    jpaEntityRepository: UserJpaEntityRepository,
    private val participantDataJpaEntityRepository: ParticipantDataJpaEntityRepository,
    private val singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository,
    private val competitionJpaEntityRepository: CompetitionJpaEntityRepository,
) : AbstractUserPersistenceAdapter<ParticipantData, SingleRoleUserId, Participant>(jpaEntityRepository),
    ParticipantRepository {

    @Transactional
    override fun save(data: ParticipantData): Participant {
        touchRoot(competitionJpaEntityRepository, data.competition.id.value)
        val (savedUserJpaEntity, savedDataJpaEntity) = persistRows(data)

        val domainEntity = ParticipantMapping.toDomain(savedUserJpaEntity, savedDataJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Participant): Participant {
        val currentDataJpaEntity = participantDataJpaEntityRepository.findByUserId(entity.id.value).requireById(entity.id.value)

        val updatedUserJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            ParticipantMapping.toUserJpaEntity(entity, current)
        }

        // The competition is fixed on creation, so the data row keeps the stored one.
        val domainEntity = ParticipantMapping.toDomain(updatedUserJpaEntity, currentDataJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun saveToCompetition(
        competitionId: CompetitionId,
        accessTokenHashes: List<AccessTokenHash>,
        nameOf: (SingleRoleUserId) -> String,
    ): List<Participant> {
        touchRoot(competitionJpaEntityRepository, competitionId.value)
        // The name depends on the id, which `persist` assigns before any row is written. All rows are added with an
        // empty name, renamed while still managed and written by the single flush below, so JDBC batching applies.
        val rows = accessTokenHashes.map { accessTokenHash ->
            persistRows(
                participantData {
                    storedAccessToken(accessTokenHash)
                    competition = competitionId
                    name = ""
                },
            )
        }

        rows.forEach { (userJpaEntity, dataJpaEntity) ->
            val participant = ParticipantMapping.toDomain(userJpaEntity, dataJpaEntity)
            val renamed = participant.withData { name = nameOf(participant.id) }
            jpaEntityRepository.save(ParticipantMapping.toUserJpaEntity(renamed, userJpaEntity))
        }
        jpaEntityRepository.flush()

        return rows.map { (userJpaEntity, dataJpaEntity) -> ParticipantMapping.toDomain(userJpaEntity, dataJpaEntity) }
    }

    override fun removeRoot(id: SingleRoleUserId, expectedVersion: Long?) {
        val dataJpaEntity = participantDataJpaEntityRepository.findByUserId(id.value) ?: return
        // Removing a participant changes the participants of its competition, as saving one does.
        touchRoot(competitionJpaEntityRepository, dataJpaEntity.competitionId)
        touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        participantDataJpaEntityRepository.delete(dataJpaEntity)
        singleRoleToUserJpaEntityRepository.deleteAll(singleRoleToUserJpaEntityRepository.findAllByUserId(id.value))
        jpaEntityRepository.deleteById(id.value)
    }

    override fun assembleSupported(rows: List<UserJpaEntity>): List<Participant> {
        val dataByUserId = findAllInChunks(
            ids = rows.map { row -> row.requireId() },
            find = participantDataJpaEntityRepository::findAllByUserIdIn,
        ).associateBy { data -> data.userId }
        return rows.mapNotNull { row -> dataByUserId[row.requireId()]?.let { data -> ParticipantMapping.toDomain(row, data) } }
    }

    /** Adds the user, role and data rows of a new participant from [data] without flushing them. */
    private fun persistRows(data: ParticipantData): Pair<UserJpaEntity, ParticipantDataJpaEntity> {
        val savedUserJpaEntity = jpaEntityRepository.save(ParticipantMapping.toUserJpaEntity(data))
        val userId = savedUserJpaEntity.requireId()

        singleRoleToUserJpaEntityRepository.save(
            SingleRoleToUserJpaEntity(singleRole = UserSingleRoleJpaEnum.PARTICIPANT, userId = userId),
        )
        val savedDataJpaEntity = participantDataJpaEntityRepository.save(ParticipantMapping.toDataJpaEntity(userId, data))
        return savedUserJpaEntity to savedDataJpaEntity
    }
}
