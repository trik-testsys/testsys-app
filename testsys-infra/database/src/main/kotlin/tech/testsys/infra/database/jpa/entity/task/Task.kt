package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
) : CompositeJpaEntity<TestToTaskContentId>(id) {

    constructor(testId: Long, taskContentId: Long): this(TestToTaskContentId(testId, taskContentId))
}

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
) : CompositeJpaEntity<DeveloperSolutionToTaskContentId>(id) {

    constructor(developerSolutionId: Long, taskContentId: Long): this(DeveloperSolutionToTaskContentId(developerSolutionId, taskContentId))
}

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
) : CompositeJpaEntity<CommunityToTaskContentId>(id) {

    constructor(communityId: Long, taskContentId: Long): this(CommunityToTaskContentId(communityId, taskContentId))
}

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
) : CompositeJpaEntity<TrikStudioVersionToTaskContentId>(id) {

    constructor(trikStudioVersionId: Long, taskContentId: Long): this(TrikStudioVersionToTaskContentId(trikStudioVersionId, taskContentId))
}

@Embeddable
data class CommunityToTaskId(
    val communityId: Long,
    val taskId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class CommunityToTaskJpaEntity(
    id: CommunityToTaskId,
) : CompositeJpaEntity<CommunityToTaskId>(id) {

    constructor(communityId: Long, taskId: Long): this(CommunityToTaskId(communityId, taskId))
}

@Entity
class TaskContent(
    val exerciseId: Long?,
    val statementId: Long?,
) : SequenceJpaEntity()

@Entity
class TaskJpaEntity(
    val name: String,
    val description: String,
    val ownerId: Long,
    @Enumerated(EnumType.STRING)
    val status: TaskStatusJpaEnum,
    val wipContentId: Long,
    val commitedContentId: Long?,
) : SequenceJpaEntity()