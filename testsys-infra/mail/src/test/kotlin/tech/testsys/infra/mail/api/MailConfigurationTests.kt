package tech.testsys.infra.mail.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.support.ResourcePropertySource
import org.springframework.mail.javamail.JavaMailSenderImpl
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.infra.mail.internal.InternalMailApi
import java.time.ZoneId
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(InternalMailApi::class)
class MailConfigurationTests {

    private val configuration = MailConfiguration()
    private val environment = StandardEnvironment().apply {
        propertySources.addFirst(ResourcePropertySource("classpath:mail-defaults.properties"))
    }

    @Nested
    inner class JavaMailSenderTests {

        @Test
        fun `should connect to the local server without credentials by default`() {
            val sender = assertIs<JavaMailSenderImpl>(configuration.javaMailSender(environment))

            assertEquals("localhost", sender.host)
            assertEquals(25, sender.port)
            assertNull(sender.username)
            assertNull(sender.password)
            assertEquals("UTF-8", sender.defaultEncoding)
            assertEquals("false", sender.javaMailProperties["mail.smtp.ssl.enable"])
        }

        @Test
        fun `should apply the timeout to connecting, reading and writing`() {
            val sender = assertIs<JavaMailSenderImpl>(configuration.javaMailSender(environment))

            assertEquals("10000", sender.javaMailProperties["mail.smtp.connectiontimeout"])
            assertEquals("10000", sender.javaMailProperties["mail.smtp.timeout"])
            assertEquals("10000", sender.javaMailProperties["mail.smtp.writetimeout"])
        }

        @Test
        fun `should use the server and credentials from the environment`() {
            environment.propertySources.addFirst(
                MapPropertySource(
                    "override",
                    mapOf(
                        "testsys.mail.host" to "smtp.example.com",
                        "testsys.mail.port" to "587",
                        "testsys.mail.username" to "robot",
                        "testsys.mail.password" to "secret",
                        "testsys.mail.smtp-auth" to "true",
                        "testsys.mail.starttls" to "true",
                    ),
                ),
            )

            val sender = assertIs<JavaMailSenderImpl>(configuration.javaMailSender(environment))

            assertEquals("smtp.example.com", sender.host)
            assertEquals(587, sender.port)
            assertEquals("robot", sender.username)
            assertEquals("secret", sender.password)
            assertEquals("true", sender.javaMailProperties["mail.smtp.auth"])
            assertEquals("false", sender.javaMailProperties["mail.smtp.ssl.enable"])
            assertEquals("true", sender.javaMailProperties["mail.smtp.starttls.enable"])
        }

        @Test
        fun `should enable SSL on port 1127 without STARTTLS when configured`() {
            environment.propertySources.addFirst(
                MapPropertySource(
                    "override",
                    mapOf(
                        "testsys.mail.port" to "1127",
                        "testsys.mail.ssl" to "true",
                        "testsys.mail.starttls" to "false",
                    ),
                ),
            )

            val sender = assertIs<JavaMailSenderImpl>(configuration.javaMailSender(environment))

            assertEquals(1127, sender.port)
            assertEquals("true", sender.javaMailProperties["mail.smtp.ssl.enable"])
            assertEquals("false", sender.javaMailProperties["mail.smtp.starttls.enable"])
        }
    }

    @Nested
    inner class MailSettingsTests {

        @Test
        fun `should render letters in the Russian region and Moscow time zone by default`() {
            val settings = configuration.mailSettings(environment)

            assertEquals(SupportedRegion.RU, settings.region)
            assertEquals(ZoneId.of("Europe/Moscow"), settings.timeZone)
            assertEquals("testsys@localhost", settings.from)
        }

        @Test
        fun `should reject a blank sender address`() {
            environment.propertySources.addFirst(MapPropertySource("override", mapOf("testsys.mail.from" to " ")))

            assertFailsWith<IllegalArgumentException> { configuration.mailSettings(environment) }
        }
    }
}
