package tech.testsys.infra.mail.api

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import tech.testsys.infra.mail.internal.InternalMailApi
import tech.testsys.infra.mail.internal.MailProperties
import tech.testsys.infra.mail.internal.MailSettings

/**
 * Registers the SMTP adapter of the user mail port with its defaults; the application overrides the
 * `testsys.mail.*` properties through the Spring Environment, which is bound once at startup.
 *
 * @since 1.0.0-beta.1
 */
@OptIn(InternalMailApi::class)
@Configuration
@ComponentScan(basePackages = ["tech.testsys.infra.mail.api", "tech.testsys.infra.mail.internal"])
@PropertySource(value = ["classpath:mail-defaults.properties"], encoding = "UTF-8")
class MailConfiguration {

    @Bean
    internal fun mailProperties(environment: Environment): MailProperties =
        Binder.get(environment).bindOrCreate(PREFIX, MailProperties::class.java)

    @Bean
    internal fun mailSettings(properties: MailProperties): MailSettings = MailSettings(
        from = properties.from,
        region = properties.region,
        timeZone = properties.timeZone,
    )

    @Bean
    internal fun javaMailSender(properties: MailProperties, environment: Environment): JavaMailSender {
        val username = properties.username?.takeIf { it.isNotBlank() }
        val password = Binder.get(environment).bind("$PREFIX.password", String::class.java).orElse(null)?.takeIf { it.isNotBlank() }

        require(!properties.isSmtpAuth || (username != null && password != null)) {
            "$PREFIX.username and $PREFIX.password must not be empty when $PREFIX.smtp-auth is true"
        }

        val timeoutMillis = properties.timeout.toMillis().toString()
        return JavaMailSenderImpl().apply {
            host = properties.host
            port = properties.port
            this.username = username
            this.password = password
            defaultEncoding = Charsets.UTF_8.name()
            javaMailProperties["mail.smtp.auth"] = properties.isSmtpAuth.toString()
            javaMailProperties["mail.smtp.ssl.enable"] = properties.isSsl.toString()
            javaMailProperties["mail.smtp.starttls.enable"] = properties.isStarttls.toString()
            javaMailProperties["mail.smtp.connectiontimeout"] = timeoutMillis
            javaMailProperties["mail.smtp.timeout"] = timeoutMillis
            javaMailProperties["mail.smtp.writetimeout"] = timeoutMillis
        }
    }

    private companion object {
        const val PREFIX = "testsys.mail"
    }
}
