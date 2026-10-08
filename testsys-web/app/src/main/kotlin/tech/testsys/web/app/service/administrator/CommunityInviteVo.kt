package tech.testsys.web.app.service.administrator

import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.group.InviteCodeHash
import java.time.Instant

/**
 * Community invite code data for pages.
 *
 * @property id the identifier of the invite.
 * @property createdAt the moment the invite was created.
 * @property kind the role granted by the invite.
 * @property codeHash the stored invite code with its hashing algorithm.
 * @property expiresAt the moment the invite stops being valid.
 * @since %CURRENT_VERSION%
 */
data class CommunityInviteVo(
    val id: CommunityInviteId,
    val createdAt: Instant,
    val kind: CommunityInvite.Kind,
    val codeHash: InviteCodeHash,
    val expiresAt: Instant,
)

internal fun CommunityInvite.toVo(): CommunityInviteVo = CommunityInviteVo(
    id = id,
    createdAt = createdAt,
    kind = kind,
    codeHash = data.codeHash,
    expiresAt = data.expiresAt,
)
