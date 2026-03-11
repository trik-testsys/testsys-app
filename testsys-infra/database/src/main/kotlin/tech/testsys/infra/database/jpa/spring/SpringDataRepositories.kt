package tech.testsys.infra.database.jpa.spring

import org.springframework.data.jpa.repository.JpaRepository
import tech.testsys.infra.database.jpa.entity.ClassJpaEntity
import tech.testsys.infra.database.jpa.entity.CommunityJpaEntity
import tech.testsys.infra.database.jpa.entity.CompetitionJpaEntity
import tech.testsys.infra.database.jpa.entity.ContestJpaEntity
import tech.testsys.infra.database.jpa.entity.DeveloperSolutionJpaEntity
import tech.testsys.infra.database.jpa.entity.ExerciseJpaEntity
import tech.testsys.infra.database.jpa.entity.JudgmentOrderJpaEntity
import tech.testsys.infra.database.jpa.entity.SolutionJpaEntity
import tech.testsys.infra.database.jpa.entity.SubmissionJpaEntity
import tech.testsys.infra.database.jpa.entity.TaskJpaEntity
import tech.testsys.infra.database.jpa.entity.TestJpaEntity
import tech.testsys.infra.database.jpa.entity.UserJpaEntity
import tech.testsys.infra.database.jpa.entity.UserRoleJpaEntity
import tech.testsys.infra.database.jpa.entity.VerdictJpaEntity

interface UserSpringRepository : JpaRepository<UserJpaEntity, Long>

interface UserRoleSpringRepository : JpaRepository<UserRoleJpaEntity, Long> {
    fun findByUserId(userId: Long): List<UserRoleJpaEntity>
    fun deleteByUserId(userId: Long)
}

interface ClassSpringRepository : JpaRepository<ClassJpaEntity, Long>

interface CommunitySpringRepository : JpaRepository<CommunityJpaEntity, Long>

interface CompetitionSpringRepository : JpaRepository<CompetitionJpaEntity, Long>

interface ContestSpringRepository : JpaRepository<ContestJpaEntity, Long>

interface TaskSpringRepository : JpaRepository<TaskJpaEntity, Long>

interface TestSpringRepository : JpaRepository<TestJpaEntity, Long>

interface SolutionSpringRepository : JpaRepository<SolutionJpaEntity, Long>

interface ExerciseSpringRepository : JpaRepository<ExerciseJpaEntity, Long>

interface DeveloperSolutionSpringRepository : JpaRepository<DeveloperSolutionJpaEntity, Long>

interface VerdictSpringRepository : JpaRepository<VerdictJpaEntity, Long>

interface SubmissionSpringRepository : JpaRepository<SubmissionJpaEntity, Long>

interface JudgmentOrderSpringRepository : JpaRepository<JudgmentOrderJpaEntity, Long>