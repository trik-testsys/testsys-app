package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * Composite primary key for [MemberToCommunityJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class MemberToCommunityId(
    val memberId: Long,
    val communityId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing a member to community association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class MemberToCommunityJpaEntity(
    id: MemberToCommunityId,
) : JpaCompositeEntity<MemberToCommunityId>(id)

/**
 * JPA entity representing a community domain entity.
 *
 * @see tech.testsys.domain.model.group.Community
 * @see tech.testsys.domain.model.group.CommunityData
 * @since %CURRENT_VERSION%
 */
@Entity
class CommunityJpaEntity(
    val ownerId: Long,
) : JpaEntity()
