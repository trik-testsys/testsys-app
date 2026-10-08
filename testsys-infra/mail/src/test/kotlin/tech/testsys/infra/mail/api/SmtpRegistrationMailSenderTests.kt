package tech.testsys.infra.mail.api

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.infra.mail.internal.InternalMailApi
import tech.testsys.infra.mail.internal.MailSettings
import java.time.ZoneId
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(InternalMailApi::class)
class SmtpRegistrationMailSenderTests {

    private val mailSender = mockk<JavaMailSender>()
    private val sender = SmtpRegistrationMailSender(
        mailSender = mailSender,
        settings = MailSettings(from = "noreply@testsys.tech", region = SupportedRegion.RU, timeZone = ZoneId.of("Europe/Moscow")),
    )
    private val message = slot<SimpleMailMessage>()

    @BeforeEach
    fun setUp() {
        every { mailSender.send(capture(message)) } just runs
    }

    @Nested
    inner class SendConfirmationCodeTests {

        @Test
        fun `should send the letter from the configured address to the email`() {
            sender.sendConfirmationCode(email = "user@example.com", confirmationCode = "012345")

            assertEquals("noreply@testsys.tech", message.captured.from)
            assertEquals(listOf("user@example.com"), message.captured.to?.toList())
        }

        @Test
        fun `should render the confirmation code subject`() {
            sender.sendConfirmationCode(email = "user@example.com", confirmationCode = "012345")

            assertEquals("Код подтверждения для регистрации в TestSys", message.captured.subject)
        }

        @Test
        fun `should put the code into the text`() {
            sender.sendConfirmationCode(email = "user@example.com", confirmationCode = "012345")

            assertContains(message.captured.text.orEmpty(), "\n\n012345\n\n")
        }

        @Test
        fun `should propagate exceptions of the mail sender`() {
            every { mailSender.send(any<SimpleMailMessage>()) } throws MailSendException("smtp is down")

            assertFailsWith<MailSendException> {
                sender.sendConfirmationCode(email = "user@example.com", confirmationCode = "012345")
            }
        }
    }

    @Nested
    inner class SendAccessTokenTests {

        @Test
        fun `should send the letter from the configured address to the email`() {
            sender.sendAccessToken(email = "user@example.com", name = "Маша", accessToken = "aB3d-x9Yz-0kLm-P7qR")

            assertEquals("noreply@testsys.tech", message.captured.from)
            assertEquals(listOf("user@example.com"), message.captured.to?.toList())
        }

        @Test
        fun `should render the access code subject`() {
            sender.sendAccessToken(email = "user@example.com", name = "Маша", accessToken = "aB3d-x9Yz-0kLm-P7qR")

            assertEquals("Ваш Код-доступа к TestSys", message.captured.subject)
        }

        @Test
        fun `should put the nickname and the access code verbatim into the text`() {
            sender.sendAccessToken(email = "user@example.com", name = "Маша", accessToken = "aB3d-x9Yz-0kLm-P7qR")

            val text = message.captured.text.orEmpty()
            assertContains(text, "Здравствуйте, Маша!")
            assertContains(text, "\n\naB3d-x9Yz-0kLm-P7qR\n\n")
        }
    }

    @Nested
    inner class DeliveryTests {

        private val transactionManager = NoOpTransactionManager()

        @AfterEach
        fun tearDown() {
            TransactionSynchronizationManager.clear()
        }

        @Test
        fun `should send the letter at once if no transaction synchronization is active`() {
            sender.sendConfirmationCode(email = "user@example.com", confirmationCode = "012345")

            verify(exactly = 1) { mailSender.send(any<SimpleMailMessage>()) }
        }

        @Test
        fun `should not send the letter before the transaction commits`() {
            transactionManager.getTransaction(DefaultTransactionDefinition())

            sender.sendConfirmationCode(email = "user@example.com", confirmationCode = "012345")

            verify(exactly = 0) { mailSender.send(any<SimpleMailMessage>()) }
        }

        @Test
        fun `should send the letter after the transaction commits`() {
            val transaction = transactionManager.getTransaction(DefaultTransactionDefinition())
            sender.sendAccessToken(email = "user@example.com", name = "Маша", accessToken = "aB3d-x9Yz-0kLm-P7qR")

            transactionManager.commit(transaction)

            assertEquals(listOf("user@example.com"), message.captured.to?.toList())
            assertContains(message.captured.text.orEmpty(), "\n\naB3d-x9Yz-0kLm-P7qR\n\n")
        }

        @Test
        fun `should send nothing if the transaction rolls back`() {
            val transaction = transactionManager.getTransaction(DefaultTransactionDefinition())
            sender.sendConfirmationCode(email = "user@example.com", confirmationCode = "012345")

            transactionManager.rollback(transaction)

            verify(exactly = 0) { mailSender.send(any<SimpleMailMessage>()) }
        }

        @Test
        fun `should not propagate a mail exception to the committing caller after the commit`() {
            every { mailSender.send(any<SimpleMailMessage>()) } throws MailSendException("smtp is down")
            val transaction = transactionManager.getTransaction(DefaultTransactionDefinition())
            sender.sendConfirmationCode(email = "user@example.com", confirmationCode = "012345")

            assertDoesNotThrow { transactionManager.commit(transaction) }
        }
    }

    /**
     * Transaction manager without a resource: it only drives Spring transaction synchronization.
     */
    private class NoOpTransactionManager : AbstractPlatformTransactionManager() {

        override fun doGetTransaction(): Any = Any()

        override fun doBegin(transaction: Any, definition: TransactionDefinition) = Unit

        override fun doCommit(status: DefaultTransactionStatus) = Unit

        override fun doRollback(status: DefaultTransactionStatus) = Unit
    }
}
