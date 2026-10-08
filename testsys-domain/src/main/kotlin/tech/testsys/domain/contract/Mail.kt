package tech.testsys.domain.contract

/**
 * Port for sending registration e-mails to users; repeated calls with the same arguments send identical letters.
 * Every call sends a new letter after the caller's active transaction commits, nothing on its rollback, and at once
 * without a transaction.
 *
 * @since %CURRENT_VERSION%
 */
interface RegistrationMailSender {

    /**
     * Sends [confirmationCode] for confirming the e-mail address [email] during registration; the letter has no
     * nickname. Technical exceptions of the adapter are propagated, except those of delivery after the commit.
     *
     * @param email the e-mail address to send the letter to.
     * @param confirmationCode the code the user enters to confirm [email].
     * @since %CURRENT_VERSION%
     */
    fun sendConfirmationCode(email: String, confirmationCode: String)

    /**
     * Sends [accessToken] assigned to the user who has just registered with [email].
     * Technical exceptions of the adapter are propagated, except those of delivery after the commit.
     *
     * @param email the e-mail address to send the letter to.
     * @param name the nickname of the user.
     * @param accessToken the original access code of the user.
     * @since %CURRENT_VERSION%
     */
    fun sendAccessToken(email: String, name: String, accessToken: String)
}
