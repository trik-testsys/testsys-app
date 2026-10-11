package tech.testsys.bootstrap

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.infra.database.api.persistence.FileStorageCleanupSettings
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingKey
import tech.testsys.infra.settings.api.SettingsChangeResult
import tech.testsys.infra.settings.api.SettingsService
import tech.testsys.infra.settings.api.SettingsStore
import tech.testsys.operation.config.CompetitionConfig
import tech.testsys.web.app.PostgresTestContainer
import tech.testsys.web.app.service.user.UserService
import java.time.Duration
import java.util.UUID

class RuntimeSettingsRecoveryTests {
    @Test
    fun `should recover last durable checkpoint before starting shared consumers`() {
        val database = PostgresTestContainer.instance
        val arguments = arrayOf(
            "--server.port=0",
            "--vaadin.productionMode=true",
            "--spring.datasource.url=${database.jdbcUrl}",
            "--spring.datasource.username=${database.username}",
            "--spring.datasource.password=${database.password}",
            "--testsys.service.secret=${"settings-recovery-".repeat(8)}",
            "--testsys.service.address=127.0.0.1",
            "--testsys.service.port=0",
        )
        val jdbc = JdbcTemplate(DriverManagerDataSource(database.jdbcUrl, database.username, database.password))
        try {
            TestSysRuntime.start(arguments, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val holder = runtime.infrastructure.getBean(RuntimeSettings::class.java)
                assertSame(holder, runtime.mainWeb.getBean(RuntimeSettings::class.java))
                assertSame(holder, runtime.serviceWeb.getBean(RuntimeSettings::class.java))
                val service = runtime.infrastructure.getBean(SettingsService::class.java)
                val users = runtime.mainWeb.getBean(UserService::class.java)
                val requests = runtime.infrastructure.getBean(RegistrationRequestRepository::class.java)
                val oldId = users.requestRegistration("settings-integration-old@example.test")
                val oldRequest = checkNotNull(requests.findById(oldId))
                val result = service.change(
                    service.overview().revision,
                    mapOf(
                        SettingKey.MaxParticipants to "7",
                        SettingKey.MaxConfirmationAttempts to "2",
                        SettingKey.ConfirmationCodeLifetime to "PT1M",
                    ),
                )
                users.requestRegistration("settings-integration-old@example.test")
                val newId = users.requestRegistration("settings-integration-new@example.test")
                assertEquals(oldRequest.data, checkNotNull(requests.findById(oldId)).data)
                assertEquals(2, checkNotNull(requests.findById(newId)).data.attemptsLeft)
                assertTrue(checkNotNull(requests.findById(newId)).data.expiresAt < oldRequest.data.expiresAt)
                assertEquals(7, runtime.infrastructure.getBean(CompetitionConfig::class.java).maxParticipants)
                assertTrue(result is SettingsChangeResult.Applied)
                // Durable commit before publication models process memory loss at this checkpoint.
                val store = runtime.infrastructure.getBean(SettingsStore::class.java)
                store.compareAndSet(store.read().revision, mapOf(SettingKey.MaxParticipants.property to "8"), UUID(0, 8))
                assertEquals(7, holder.snapshot().values.int(SettingKey.MaxParticipants))
            }
            TestSysRuntime.start(arguments, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val holder = runtime.infrastructure.getBean(RuntimeSettings::class.java)
                assertEquals(8, holder.snapshot().values.int(SettingKey.MaxParticipants))
                val store = runtime.infrastructure.getBean(SettingsStore::class.java)
                val previous = store.read()
                val committed = checkNotNull(
                    store.compareAndSet(previous.revision, mapOf(SettingKey.MaxParticipants.property to "9"), UUID(0, 9)),
                )
                store.compareAndSet(committed.revision, previous.overrides, UUID(0, 10), committed.changeId)
            }
            TestSysRuntime.start(arguments, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                assertEquals(8, runtime.infrastructure.getBean(CompetitionConfig::class.java).maxParticipants)
            }
        } finally {
            jdbc.execute("truncate table ts_runtime_settings")
            jdbc.update(
                "delete from ts_registration_request where email in (?, ?)",
                "settings-integration-old@example.test",
                "settings-integration-new@example.test",
            )
        }
    }

    // Review of runtime settings storage: the restored cleanup settings were rebound from the environment (sysdev-cabinet branch).
    @Test
    @Tag("regression")
    fun `should start with restored cleanup settings overriding invalid environment values`() {
        val database = PostgresTestContainer.instance
        val arguments = arrayOf(
            "--server.port=0",
            "--vaadin.productionMode=true",
            "--spring.datasource.url=${database.jdbcUrl}",
            "--spring.datasource.username=${database.username}",
            "--spring.datasource.password=${database.password}",
            "--testsys.service.secret=${"settings-recovery-".repeat(8)}",
            "--testsys.service.address=127.0.0.1",
            "--testsys.service.port=0",
        )
        val invalidEnvironment = arguments + arrayOf(
            "--testsys.file-storage.cleanup.batch-size=invalid",
            "--testsys.file-storage.cleanup.minimum-age=invalid",
        )
        val jdbc = JdbcTemplate(DriverManagerDataSource(database.jdbcUrl, database.username, database.password))
        try {
            TestSysRuntime.start(arguments, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val service = runtime.infrastructure.getBean(SettingsService::class.java)
                val result = service.change(
                    service.overview().revision,
                    mapOf(SettingKey.CleanupBatchSize to "3", SettingKey.CleanupMinimumAge to "PT2H"),
                )
                assertTrue(result is SettingsChangeResult.Applied)
            }

            TestSysRuntime.start(invalidEnvironment, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val actual = runtime.infrastructure.getBean(FileStorageCleanupSettings::class.java)

                assertEquals(FileStorageCleanupSettings(Duration.ofHours(2), 3), actual)
            }
        } finally {
            jdbc.execute("truncate table ts_runtime_settings")
        }
    }
}

@TestConfiguration(proxyBeanMethods = false)
class RuntimeSettingsTestConfiguration {
    @Bean
    @Primary
    fun mailSender(): UserMailSender = mockk(relaxed = true)
}
