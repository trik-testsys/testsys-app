package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import java.time.Instant

/**
 * Builder of [CommunityInviteData]. Required: [code] or [storedCode], [expiresAt].
 *
 * @property expiresAt the moment the invite stops being valid, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class CommunityInviteDataBuilder : InviteCodeDataBuilder<CommunityInviteData>() {

    var expiresAt: Instant? = null

    override fun build(): CommunityInviteData {
        val codeHash = requireCodeHash()
        val expiresAt = requireField(expiresAt) { ::expiresAt }

        return CommunityInviteData(
            codeHash = codeHash,
            expiresAt = expiresAt,
        )
    }
}

/**
 * Base class of builders of [CommunityInvite] variants. Required: [id], [createdAt], [data].
 *
 * @param Invite the type of the built variant.
 * @since %CURRENT_VERSION%
 */
abstract class CommunityInviteBuilder<Invite : CommunityInvite> :
    DomainEntityWithDataBuilder<Invite, CommunityInviteData, CommunityInviteDataBuilder>() {

    override fun dataBuilder() = CommunityInviteDataBuilder()

    override fun build(): Invite {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return create(id = CommunityInviteId(id), createdAt = createdAt, data = data).applyVersion(version)
    }

    protected abstract fun create(id: CommunityInviteId, createdAt: Instant, data: CommunityInviteData): Invite
}

/**
 * Builder of [CommunityInvite.Manager] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class ManagerCommunityInviteBuilder : CommunityInviteBuilder<CommunityInvite.Manager>() {

    override fun create(id: CommunityInviteId, createdAt: Instant, data: CommunityInviteData) =
        CommunityInvite.Manager(id = id, createdAt = createdAt, data = data)
}

/**
 * Builder of [CommunityInvite.Developer] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class DeveloperCommunityInviteBuilder : CommunityInviteBuilder<CommunityInvite.Developer>() {

    override fun create(id: CommunityInviteId, createdAt: Instant, data: CommunityInviteData) =
        CommunityInvite.Developer(id = id, createdAt = createdAt, data = data)
}
