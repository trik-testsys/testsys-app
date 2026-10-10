package tech.testsys.infra.mail.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.infra.mail.internal.InternalMailApi
import tech.testsys.infra.mail.internal.MailSettings
import java.time.Duration
import java.time.ZoneId

/**
 * Registers the SMTP adapter of the user mail port with its defaults; the application overrides the
 * `testsys.mail.*` properties through the Spring Environment.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalMailApi::class)
@Configuration
@ComponentScan(basePackages = ["tech.testsys.infra.mail.api", "tech.testsys.infra.mail.internal"])
@PropertySource(value = ["classpath:mail-defaults.properties"], encoding = "UTF-8")
class MailConfiguration {

    @Bean
    internal fun mailSettings(environment: Environment): MailSettings = MailSettings(
        from = environment.getRequiredProperty("testsys.mail.from"),
        region = SupportedRegion.valueOf(environment.getRequiredProperty("testsys.mail.region")),
        timeZone = ZoneId.of(environment.getRequiredProperty("testsys.mail.time-zone")),
    )

    @Bean
    internal fun javaMailSender(environment: Environment): JavaMailSender {
        val timeoutMillis = Duration.parse(environment.getRequiredProperty("testsys.mail.timeout")).toMillis().toString()
        return JavaMailSenderImpl().apply {
            host = environment.getRequiredProperty("testsys.mail.host")
            port = environment.getRequiredProperty("testsys.mail.port", Int::class.java)
            username = environment.getProperty("testsys.mail.username")?.takeIf { it.isNotBlank() }
            password = environment.getProperty("testsys.mail.password")?.takeIf { it.isNotBlank() }
            defaultEncoding = Charsets.UTF_8.name()
            javaMailProperties["mail.smtp.auth"] = environment.getRequiredProperty("testsys.mail.smtp-auth")
            javaMailProperties["mail.smtp.ssl.enable"] = environment.getRequiredProperty("testsys.mail.ssl")
            javaMailProperties["mail.smtp.starttls.enable"] = environment.getRequiredProperty("testsys.mail.starttls")
            javaMailProperties["mail.smtp.connectiontimeout"] = timeoutMillis
            javaMailProperties["mail.smtp.timeout"] = timeoutMillis
            javaMailProperties["mail.smtp.writetimeout"] = timeoutMillis
        }
    }
}
