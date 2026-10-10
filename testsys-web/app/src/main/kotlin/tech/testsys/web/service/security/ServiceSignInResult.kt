package tech.testsys.web.service.security

/**
 * Outcome of a service login attempt, without the submitted secret.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ServiceSignInResult {
    /**
     * The submitted code was accepted.
     *
     * @since %CURRENT_VERSION%
     */
    data object Success : ServiceSignInResult

    /**
     * The submitted code was refused without blocking the address yet.
     *
     * @since %CURRENT_VERSION%
     */
    data object InvalidCode : ServiceSignInResult

    /**
     * The address is blocked, including the fifth failure that starts the block.
     *
     * @since %CURRENT_VERSION%
     */
    data object Blocked : ServiceSignInResult
}
