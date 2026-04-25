package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing a community domain entity.
 *
 * @see tech.testsys.domain.model.group.Community
 * @see tech.testsys.domain.model.group.CommunityData
 * @since %CURRENT_VERSION%
 */
@Entity
class CommunityJpaEntity(
    name: String,
    description: String,
    val ownerId: Long,
) : SequenceJpaEntity()
