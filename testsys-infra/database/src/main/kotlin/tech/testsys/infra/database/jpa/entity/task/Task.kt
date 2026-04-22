package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.jpa.entity.DescribableJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity


enum class TaskStatusJpaEnum {
    NEW,
    UNCOMMITED,
    COMMITED
}

@Embeddable
data class TestToTaskContentId(
    val testId: Long,
    val taskContentId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class TestToTaskContentJpaEntity(
    id: TestToTaskContentId,
) : CompositeJpaEntity<TestToTaskContentId>(id)

@Embeddable
data class DeveloperSolutionToTaskContentId(
    val developerSolutionId: Long,
    val taskContentId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class DeveloperSolutionToTaskContentJpaEntity(
    id: DeveloperSolutionToTaskContentId,
) : CompositeJpaEntity<DeveloperSolutionToTaskContentId>(id)

@Embeddable
data class CommunityToTaskContentId(
    val communityId: Long,
    val taskContentId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class CommunityToTaskContentJpaEntity(
    id: CommunityToTaskContentId,
) : CompositeJpaEntity<CommunityToTaskContentId>(id)

@Embeddable
data class TrikStudioVersionToTaskContentId(
    val trikStudioVersionId: Long,
    val taskContentId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class TrikStudioVersionToTaskContentJpaEntity(
    id: TrikStudioVersionToTaskContentId,
) : CompositeJpaEntity<TrikStudioVersionToTaskContentId>(id)

@Entity
class TaskContent(
    name: String,
    description: String,
    val ownerId: Long,
    val exerciseId: Long?,
    val statementId: Long?,
) : DescribableJpaEntity(name, description)

@Entity
class TaskJpaEntity(
    @Enumerated(EnumType.STRING)
    val status: TaskStatusJpaEnum,
    val wipContentId: Long,
    val commitedContentId: Long,
) : SequenceJpaEntity()