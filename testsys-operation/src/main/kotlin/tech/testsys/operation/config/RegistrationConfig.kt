package tech.testsys.operation.config

import tech.testsys.operation.annotation.OperationConfig
import java.time.Duration

/**
 * Configuration of self-registration of users.
 *
 * @property confirmationCodeLifetime how long a newly issued confirmation code is accepted.
 * @property maxConfirmationAttempts the number of attempts to enter a newly issued confirmation code.
 * @since %CURRENT_VERSION%
 */
@OperationConfig("registration")
interface RegistrationConfig {

    val confirmationCodeLifetime: Duration

    val maxConfirmationAttempts: Int
}
