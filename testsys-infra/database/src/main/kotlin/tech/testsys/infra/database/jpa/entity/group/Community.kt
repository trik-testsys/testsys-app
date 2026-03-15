package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaEntity

@Embeddable
data class MemberToCommunityId(
    val memberId: Long,
    val communityId: Long,
) : JpaCompositeId()

@Entity
class MemberToCommunityJpaEntity(
    id: MemberToCommunityId,
) : JpaCompositeEntity<MemberToCommunityId>(id)

@Entity
class CommunityJpaEntity(
    val ownerId: Long,
) : JpaEntity()
