package tech.testsys.infra.mail.api

import org.apache.commons.logging.LogFactory
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.domain.contract.RegistrationMailSender
import tech.testsys.infra.localization.Localization
import tech.testsys.infra.mail.internal.InternalMailApi
import tech.testsys.infra.mail.internal.MailSettings

/**
 * Sends registration letters as plain-text e-mails through [JavaMailSender], rendered in the configured region;
 * within active transaction synchronization a letter is sent after the commit and a failure is only logged.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalMailApi::class)
class SmtpRegistrationMailSender(
    private val mailSender: JavaMailSender,
    private val settings: MailSettings,
) : RegistrationMailSender {

    override fun sendConfirmationCode(email: String, confirmationCode: String) {
        val messages = Localization.forRegion(settings.region, settings.timeZone).user
        send(
            to = email,
            subject = messages.registrationConfirmationCodeSubject(),
            text = messages.registrationConfirmationCodeBody(confirmationCode = confirmationCode),
        )
    }

    override fun sendAccessToken(email: String, name: String, accessToken: String) {
        val messages = Localization.forRegion(settings.region, settings.timeZone).user
        send(
            to = email,
            subject = messages.registrationAccessCodeSubject(),
            text = messages.registrationAccessCodeBody(name = name, accessCode = accessToken),
        )
    }

    private fun send(to: String, subject: String, text: String) {
        val message = SimpleMailMessage().apply {
            from = settings.from
            setTo(to)
            this.subject = subject
            this.text = text
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() = sendCommitted(message, to)
                },
            )
        } else {
            mailSender.send(message)
        }
    }

    /**
     * Sends [message] after the commit. The registration is already committed, so a failure is logged instead of
     * reaching the committing caller.
     */
    private fun sendCommitted(message: SimpleMailMessage, to: String) {
        try {
            mailSender.send(message)
        } catch (exception: MailException) {
            log.error("Failed to send registration letter to $to after transaction commit", exception)
        }
    }

    private companion object {
        private val log = LogFactory.getLog(SmtpRegistrationMailSender::class.java)
    }
}
