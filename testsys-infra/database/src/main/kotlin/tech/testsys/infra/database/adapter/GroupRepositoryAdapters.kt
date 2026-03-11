package tech.testsys.infra.database.adapter

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Repository
import tech.testsys.domain.contract.EntityRepository
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.LazyNullableEntity
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.jpa.entity.ClassJpaEntity
import tech.testsys.infra.database.jpa.entity.CommunityJpaEntity
import tech.testsys.infra.database.jpa.entity.CompetitionJpaEntity
import tech.testsys.infra.database.jpa.spring.ClassSpringRepository
import tech.testsys.infra.database.jpa.spring.CommunitySpringRepository
import tech.testsys.infra.database.jpa.spring.CompetitionSpringRepository

@Repository
class ClassRepositoryAdapter(
    private val classRepo: ClassSpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<ClassData, ClassId, Class> {

    override fun load(field: LazyEntity<ClassId, Class>): Class {
        val jpa: ClassJpaEntity = classRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Class ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<ClassId, Class>): Class? =
        classRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<ClassId, Class>, pageSize: Int, page: Int): List<Class> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return classRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: ClassData): Class {
        val jpa = ClassJpaEntity(
            ownerId = data.owner.id.value,
            studentIds = data.students.ids.map { it.value }.toMutableList(),
            contestIds = data.contests.ids.map { it.value }.toMutableList(),
        )
        val saved = classRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<ClassData>): List<Class> = data.map { save(it) }

    private fun toDomain(jpa: ClassJpaEntity): Class {
        val owner = userEntityLoader.loadById(jpa.ownerId) as MultipleRoleUser
        return Class(
            id = ClassId(jpa.id),
            data = ClassData(
                owner = owner,
                students = LazyEntityList(jpa.studentIds.map { UserId(it) }),
                contests = LazyEntityList(jpa.contestIds.map { ContestId(it) }),
            )
        )
    }
}

@Repository
class CommunityRepositoryAdapter(
    private val communityRepo: CommunitySpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<CommunityData, CommunityId, Community> {

    override fun load(field: LazyEntity<CommunityId, Community>): Community {
        val jpa: CommunityJpaEntity = communityRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Community ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<CommunityId, Community>): Community? =
        communityRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<CommunityId, Community>, pageSize: Int, page: Int): List<Community> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return communityRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: CommunityData): Community {
        val jpa = CommunityJpaEntity(
            ownerId = data.owner.id.value,
            memberIds = data.members.ids.map { it.value }.toMutableList(),
        )
        val saved = communityRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<CommunityData>): List<Community> = data.map { save(it) }

    private fun toDomain(jpa: CommunityJpaEntity): Community {
        val owner = userEntityLoader.loadById(jpa.ownerId) as MultipleRoleUser
        return Community(
            id = CommunityId(jpa.id),
            data = CommunityData(
                owner = owner,
                members = LazyEntityList(jpa.memberIds.map { UserId(it) }),
            )
        )
    }
}

@Repository
class CompetitionRepositoryAdapter(
    private val competitionRepo: CompetitionSpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<CompetitionData, CompetitionId, Competition> {

    override fun load(field: LazyEntity<CompetitionId, Competition>): Competition {
        val jpa: CompetitionJpaEntity = competitionRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Competition ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<CompetitionId, Competition>): Competition? =
        competitionRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(
        list: LazyEntityList<CompetitionId, Competition>,
        pageSize: Int,
        page: Int,
    ): List<Competition> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return competitionRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: CompetitionData): Competition {
        val jpa = CompetitionJpaEntity(
            ownerId = data.owner.id.value,
            participantIds = data.participants.ids.map { it.value }.toMutableList(),
            contestIds = data.contests.ids.map { it.value }.toMutableList(),
        )
        val saved = competitionRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<CompetitionData>): List<Competition> = data.map { save(it) }

    private fun toDomain(jpa: CompetitionJpaEntity): Competition {
        val owner = userEntityLoader.loadById(jpa.ownerId) as MultipleRoleUser
        return Competition(
            id = CompetitionId(jpa.id),
            data = CompetitionData(
                owner = owner,
                participants = LazyEntityList(jpa.participantIds.map { UserId(it) }),
                contests = LazyEntityList(jpa.contestIds.map { ContestId(it) }),
            )
        )
    }
}