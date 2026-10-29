package tech.testsys.web.service.security

/**
 * Outcome of a service login attempt, without the submitted secret.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ServiceSignInResult {
    /**
     * The submitted code was accepted.
     *
     * @since 1.0.0-beta.1
     */
    data object Success : ServiceSignInResult

    /**
     * The submitted code was refused without blocking the address yet.
     *
     * @since 1.0.0-beta.1
     */
    data object InvalidCode : ServiceSignInResult

    /**
     * The address is blocked, including the fifth failure that starts the block.
     *
     * @since 1.0.0-beta.1
     */
    data object Blocked : ServiceSignInResult
}
