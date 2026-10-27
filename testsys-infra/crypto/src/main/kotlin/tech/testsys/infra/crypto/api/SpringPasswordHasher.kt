package tech.testsys.infra.crypto.api

import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.stereotype.Component
import tech.testsys.domain.contract.PasswordHasher
import tech.testsys.domain.model.user.PasswordHash

/**
 * Password hashing adapter backed by the Spring Security delegating encoder, which stores bcrypt hashes with
 * their algorithm prefix; passwords longer than bcrypt accepts are never hashed and never match.
 *
 * @since %CURRENT_VERSION%
 */
@Component
class SpringPasswordHasher : PasswordHasher {

    private val encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    // Computed once so that checks without a stored hash spend the same bcrypt work as real ones.
    private val dummyHash = encode(DUMMY_PASSWORD)

    override fun hash(password: String): PasswordHash {
        require(password.isHashable()) { "Password exceeds $MAX_PASSWORD_BYTES bytes in UTF-8 and cannot be hashed" }
        return PasswordHash(encode(password))
    }

    override fun matches(password: String, passwordHash: PasswordHash?): Boolean {
        if (passwordHash == null || !password.isHashable()) {
            encoder.matches(DUMMY_PASSWORD, dummyHash)
            return false
        }

        return encoder.matches(password, passwordHash.value)
    }

    private fun encode(password: String): String = checkNotNull(encoder.encode(password)) { "Password encoder returned no hash" }

    private fun String.isHashable(): Boolean = toByteArray(Charsets.UTF_8).size <= MAX_PASSWORD_BYTES

    private companion object {
        const val MAX_PASSWORD_BYTES = 72
        const val DUMMY_PASSWORD = "testsys-dummy-password"
    }
}
