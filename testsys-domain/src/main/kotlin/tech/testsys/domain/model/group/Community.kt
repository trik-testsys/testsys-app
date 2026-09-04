package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Identifier of a [Community].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class CommunityId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Community]. Membership is stored on the members' side, not here.
 *
 * @property owner the administrator who owns the community.
 * @property name the name of the community.
 * @property description the description of the community.
 * @since %CURRENT_VERSION%
 */
data class CommunityData(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val name: String,
    val description: String,
)

/**
 * A named set of users: the unit of role membership and the target that tasks and contests are shared to.
 *
 * @property data the data of the community.
 * @since %CURRENT_VERSION%
 */
class Community(
    id: CommunityId,
    createdAt: Instant,
    version: EntityVersion,
    val data: CommunityData,
) : DomainEntity<CommunityId>(id, createdAt, version)
