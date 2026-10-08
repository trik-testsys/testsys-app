package tech.testsys.domain.contract

/**
 * Port for sending e-mails to users; repeated calls with the same arguments send identical letters.
 * Every call sends a new letter after the caller's active transaction commits, nothing on its rollback, and at once
 * without a transaction.
 *
 * @since %CURRENT_VERSION%
 */
interface UserMailSender {

    /**
     * Sends [confirmationCode] for confirming the e-mail address [email] during registration; the letter has no
     * nickname. Technical exceptions of the adapter are propagated, except those of delivery after the commit.
     *
     * @param email the e-mail address to send the letter to.
     * @param confirmationCode the code the user enters to confirm [email].
     * @since %CURRENT_VERSION%
     */
    fun sendRegistrationConfirmationCode(email: String, confirmationCode: String)

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

    /**
     * Sends [confirmationCode] for confirming the new e-mail address [email] of a user changing it; the letter has no
     * nickname. Technical exceptions of the adapter are propagated, except those of delivery after the commit.
     *
     * @param email the new e-mail address to send the letter to.
     * @param confirmationCode the code the user enters to confirm [email].
     * @since %CURRENT_VERSION%
     */
    fun sendEmailChangeConfirmationCode(email: String, confirmationCode: String)

    /**
     * Notifies the user [name] at the previous e-mail address [email] that their address has been changed; the letter
     * does not contain the new address. Technical exceptions of the adapter are propagated, except those of delivery
     * after the commit.
     *
     * @param email the previous e-mail address to send the letter to.
     * @param name the nickname of the user.
     * @since %CURRENT_VERSION%
     */
    fun sendEmailChangedNotice(email: String, name: String)
}
