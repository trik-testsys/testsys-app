package tech.testsys.domain.model.user

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Marker for identifiers of [User]s.
 *
 * @since %CURRENT_VERSION%
 */
interface UserId : DomainId

/**
 * Sealed base of every user of the system. `data` is only a constructor parameter here; each concrete
 * user kind exposes its own typed `data` property.
 *
 * @param Id the identifier type of the concrete user kind.
 * @since %CURRENT_VERSION%
 */
sealed class User<Id : UserId>(
    id: Id,
    createdAt: Instant,
    data: UserData,
) : DomainEntity<Id>(id, createdAt)

/**
 * Algorithm used to produce the stored access-code representation of a [SingleRoleUser] and the stored invite-code
 * representation.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface HashAlgorithm {

    /**
     * Temporary algorithm of invite codes that leaves the original code unchanged and provides no cryptographic
     * protection. Declarations marked with RawInviteCodeDependency must be checked before invite codes use another one.
     *
     * @since %CURRENT_VERSION%
     */
    object Identity : HashAlgorithm

    /**
     * HMAC-SHA-256 keyed with the server secret and written as lower-case hex; access codes use it.
     *
     * @since %CURRENT_VERSION%
     */
    object HmacSha256 : HashAlgorithm
}

/**
 * Stored access-code representation of a [SingleRoleUser] and the algorithm that produced it.
 *
 * @property value the stored representation.
 * @property algorithm the algorithm used to produce [value].
 * @since %CURRENT_VERSION%
 */
data class AccessTokenHash(
    val value: String,
    val algorithm: HashAlgorithm,
)

/**
 * Hashes access codes of [SingleRoleUser]s with [HashAlgorithm.HmacSha256] keyed with the UTF-8 bytes of [secret] of
 * at least 64 characters. The hash is deterministic, so it finds the user by the entered code but cannot restore it.
 *
 * @since %CURRENT_VERSION%
 */
class AccessTokenHasher(secret: String) {

    private val key: SecretKeySpec

    init {
        require(secret.length >= MIN_SECRET_LENGTH) {
            "Access code secret must contain at least $MIN_SECRET_LENGTH characters, got ${secret.length}"
        }
        key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), MAC_ALGORITHM)
    }

    /**
     * Hashes [rawAccessToken] without normalizing or validating the input.
     *
     * @since %CURRENT_VERSION%
     */
    fun hash(rawAccessToken: String): AccessTokenHash {
        val mac = Mac.getInstance(MAC_ALGORITHM).apply { init(key) }
        val value = HexFormat.of().formatHex(mac.doFinal(rawAccessToken.toByteArray(Charsets.UTF_8)))
        return AccessTokenHash(value = value, algorithm = HashAlgorithm.HmacSha256)
    }

    private companion object {
        const val MIN_SECRET_LENGTH = 64
        const val MAC_ALGORITHM = "HmacSHA256"
    }
}

/**
 * Data common to every kind of [User].
 *
 * @property name the name of the user.
 * @since %CURRENT_VERSION%
 */
interface UserData {
    val name: String
}
