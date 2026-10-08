package tech.testsys.operation.config

import tech.testsys.operation.annotation.OperationConfig
import java.time.Duration

/**
 * Configuration of confirming an e-mail address by a code during self-registration and e-mail change.
 *
 * @property confirmationCodeLifetime how long a newly issued confirmation code is accepted.
 * @property maxConfirmationAttempts the number of attempts to enter a newly issued confirmation code.
 * @since %CURRENT_VERSION%
 */
@OperationConfig("email-confirmation")
interface EmailConfirmationConfig {

    val confirmationCodeLifetime: Duration

    val maxConfirmationAttempts: Int
}
