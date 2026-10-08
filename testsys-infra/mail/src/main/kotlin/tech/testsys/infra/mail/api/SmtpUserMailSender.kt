package tech.testsys.infra.mail.api

import org.apache.commons.logging.LogFactory
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.infra.localization.Localization
import tech.testsys.infra.mail.internal.InternalMailApi
import tech.testsys.infra.mail.internal.MailSettings

/**
 * Sends letters to users as plain-text e-mails through [JavaMailSender], rendered in the configured region;
 * within active transaction synchronization a letter is sent after the commit and a failure is only logged.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalMailApi::class)
class SmtpUserMailSender(
    private val mailSender: JavaMailSender,
    private val settings: MailSettings,
) : UserMailSender {

    override fun sendRegistrationConfirmationCode(email: String, confirmationCode: String) {
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

    override fun sendEmailChangeConfirmationCode(email: String, confirmationCode: String) {
        val messages = Localization.forRegion(settings.region, settings.timeZone).user
        send(
            to = email,
            subject = messages.emailChangeConfirmationCodeSubject(),
            text = messages.emailChangeConfirmationCodeBody(confirmationCode = confirmationCode),
        )
    }

    override fun sendEmailChangedNotice(email: String, name: String) {
        val messages = Localization.forRegion(settings.region, settings.timeZone).user
        send(
            to = email,
            subject = messages.emailChangeNoticeSubject(),
            text = messages.emailChangeNoticeBody(name = name),
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
     * Sends [message] after the commit. The changes are already committed, so a failure is logged instead of
     * reaching the committing caller.
     */
    private fun sendCommitted(message: SimpleMailMessage, to: String) {
        try {
            mailSender.send(message)
        } catch (exception: MailException) {
            log.error("Failed to send letter to $to after transaction commit", exception)
        }
    }

    private companion object {
        private val log = LogFactory.getLog(SmtpUserMailSender::class.java)
    }
}
