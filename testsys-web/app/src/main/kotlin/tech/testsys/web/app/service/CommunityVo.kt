package tech.testsys.web.app.service

import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Community data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the community.
 * @property createdAt the moment the community was created.
 * @property owner the identifier of the administrator who created the community.
 * @property name the name of the community.
 * @property description the description of the community.
 * @property managerInvite the identifier of the invite code for the manager role.
 * @property developerInvite the identifier of the invite code for the developer role.
 * @since %CURRENT_VERSION%
 */
data class CommunityVo(
    val id: CommunityId,
    val createdAt: Instant,
    val owner: MultipleRoleUserId,
    val name: String,
    val description: String,
    val managerInvite: CommunityInviteId,
    val developerInvite: CommunityInviteId,
)

internal fun Community.toVo(): CommunityVo = CommunityVo(
    id = id,
    createdAt = createdAt,
    owner = data.owner.id,
    name = data.name,
    description = data.description,
    managerInvite = data.managerInvite.id,
    developerInvite = data.developerInvite.id,
)
