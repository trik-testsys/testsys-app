package tech.testsys.web.service

import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.mock.env.MockEnvironment
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.grpc.api.GradingNodeImportPreview
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GradingNodeRegistryOverview
import tech.testsys.infra.grpc.api.GradingQueueMonitor
import tech.testsys.infra.grpc.api.GradingQueueSnapshot
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingKey
import tech.testsys.infra.settings.api.SettingsOverview
import tech.testsys.infra.settings.api.SettingsPersistence
import tech.testsys.infra.settings.api.SettingsService
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.SettingsValues
import java.time.Clock

const val SERVICE_TEST_SECRET: String = "test-service-code-that-is-only-used-by-automated-tests-0123456789-ABCD"

@TestConfiguration(proxyBeanMethods = false)
class ServiceTestConfiguration {
    @Bean
    fun runtimeSettings(): RuntimeSettings {
        val values = SettingKey.entries.associateWith { key ->
            key.defaultValue ?: when (key) {
                SettingKey.MaxParticipants -> "100"
                SettingKey.ConfirmationCodeLifetime, SettingKey.CommunityInviteTtl, SettingKey.ClassInviteTtl -> "PT5M"
                else -> "5"
            }
        }
        return RuntimeSettings(SettingsSnapshot(0, SettingsValues(values), 400_000_000), CommunityId(42))
    }

    @Bean
    fun settingsService(runtime: RuntimeSettings): SettingsService = mockk {
        every { overview() } returns SettingsOverview(0, runtime.snapshot(), SettingsPersistence.Known)
    }

    @Bean
    fun gradingNodeRegistry(): GradingNodeRegistry = mockk {
        every { overview() } returns GradingNodeRegistryOverview(emptyList(), SettingsPersistence.Known)
        every { importPreview() } returns GradingNodeImportPreview(emptyList())
    }

    @Bean
    fun gradingQueueMonitor(): GradingQueueMonitor = mockk {
        every { snapshot() } returns GradingQueueSnapshot(preparing = 0, waiting = 0, grading = 0, saving = 0)
    }

    @Bean
    fun serviceSettings(environment: Environment): ServiceWebSettings = ServiceWebSettings.from(
        MockEnvironment().withProperty("testsys.service.secret", SERVICE_TEST_SECRET)
            .withProperty("testsys.service.trusted-proxies", environment.getProperty("testsys.service.trusted-proxies", ""))
            .withProperty("testsys.service.address", "127.0.0.1").withProperty("testsys.service.port", "0"),
    )

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
