package tech.testsys.operation.config

import tech.testsys.domain.model.group.CommunityId

/**
 * Identity of the technical owner of the public community.
 *
 * @property EMAIL the address reserved for that owner.
 * @since 1.0.0-beta.1
 */
object PublicCommunityOwner {
    const val EMAIL = "public-community-owner@testsys.invalid"
}

/**
 * Configuration of the communities operations rely on.
 *
 * @property publicCommunityId the id of the public community every self-registered user joins.
 * @since 1.0.0-beta.1
 */
interface CommunityConfig {

    val publicCommunityId: CommunityId
}
