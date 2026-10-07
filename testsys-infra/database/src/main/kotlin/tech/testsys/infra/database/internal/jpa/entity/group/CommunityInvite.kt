package tech.testsys.infra.database.internal.jpa.entity.group

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import java.time.Instant

/**
 * Role granted by a [CommunityInviteJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class CommunityInviteRoleJpaEnum {

    MANAGER,
    DEVELOPER,
}

/**
 * JPA entity of [tech.testsys.domain.model.group.CommunityInvite].
 *
 * @property role the variant of the invite: the role granted in the community.
 * @property code the stored invite-code representation.
 * @property codeHashAlgorithm the algorithm used to produce the stored invite-code representation.
 * @property expiresAt the moment the invite stops being valid.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
@RawInviteCodeDependency(
    reason = "The uk_ts_community_invite_code constraint enforces original invite-code uniqueness only with Identity.",
)
class CommunityInviteJpaEntity(
    @Enumerated(EnumType.STRING)
    val role: CommunityInviteRoleJpaEnum,
    val code: String,
    @Enumerated(EnumType.STRING)
    val codeHashAlgorithm: HashAlgorithmJpaEnum,
    val expiresAt: Instant,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
