package tech.testsys.operation.config

import java.time.Duration

/**
 * Configuration of confirming an e-mail address by a code during self-registration, e-mail change and access restoration.
 *
 * @property confirmationCodeLifetime how long a newly issued confirmation code is accepted.
 * @property maxConfirmationAttempts the number of attempts to enter a newly issued confirmation code.
 * @since 1.0.0-beta.1
 */
interface EmailConfirmationConfig {

    val confirmationCodeLifetime: Duration

    val maxConfirmationAttempts: Int
}
