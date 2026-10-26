package tech.testsys.web.service

import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.mock.env.MockEnvironment
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.supervisor
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.User
import tech.testsys.infra.database.api.service.ServiceTrikStudioVersions
import tech.testsys.infra.database.api.service.ServiceUserId
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.infra.grpc.api.GradingNodeImportPreview
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GradingNodeRegistryOverview
import tech.testsys.infra.grpc.api.GradingQueueMonitor
import tech.testsys.infra.grpc.api.GradingQueueSnapshot
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingsValues
import tech.testsys.infra.settings.api.service.SettingsOverview
import tech.testsys.infra.settings.api.service.SettingsPersistence
import tech.testsys.infra.settings.api.service.SettingsService
import java.time.Clock
import java.time.Instant

const val SERVICE_TEST_SECRET: String = "test-service-code-that-is-only-used-by-automated-tests-0123456789-ABCD"

const val SERVICE_TEST_GRAFANA_URL: String = "https://grafana.example.test/monitoring/"

/** Users of every kind that the mocked service user search finds by id. */
val SERVICE_TEST_USERS: List<User<*>> = listOf(
    multipleRoleUser {
        id = 1001
        createdAt = Instant.parse("2026-03-14T09:30:00Z")
        data = multipleRoleUserData {
            accessToken("multiple-role-token", algorithm = HashAlgorithm.Identity)
            name = "Мария Иванова"
            email = "maria@example.com"
            roles {
                administrator {}
                student { data = studentData {} }
            }
        }
    },
    participant {
        id = 1002
        createdAt = Instant.parse("2026-03-14T09:30:00Z")
        data = participantData {
            competition(2001)
            accessToken("participant-token", algorithm = HashAlgorithm.Identity)
            name = "Участник Пётр"
        }
    },
    observer {
        id = 1003
        createdAt = Instant.parse("2026-03-14T09:30:00Z")
        data = observerData {
            community(3001)
            contests = mutableListOf(ContestId(4001), ContestId(4002))
            accessToken("observer-token", algorithm = HashAlgorithm.Identity)
            name = "Наблюдатель Анна"
        }
    },
    supervisor {
        id = 1004
        createdAt = Instant.parse("2026-03-14T09:30:00Z")
        data = supervisorData {
            accessToken("supervisor-token", algorithm = HashAlgorithm.Identity)
            name = "Супервайзер Олег"
        }
    },
)

/** Makes the mocked [search] find nothing by conditions and find each of [SERVICE_TEST_USERS] by its id. */
fun stubServiceTestUsers(search: ServiceUserSearch) {
    every { search.find(any(), any()) } answers { Page(emptyList(), secondArg(), 0) }
    every { search.findById(any()) } returns null
    SERVICE_TEST_USERS.forEach { user -> every { search.findById(ServiceUserId(user.id.value)) } returns user }
}

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
    fun serviceUserSearch(): ServiceUserSearch = mockk<ServiceUserSearch>().also(::stubServiceTestUsers)

    @Bean
    fun serviceTrikStudioVersions(): ServiceTrikStudioVersions = mockk { every { findAll() } returns emptyList() }

    @Bean
    fun serviceSettings(): ServiceWebSettings = ServiceWebSettings.from(
        MockEnvironment().withProperty("testsys.service.secret", SERVICE_TEST_SECRET)
            .withProperty("testsys.service.address", "127.0.0.1").withProperty("testsys.service.port", "0")
            .withProperty("testsys.service.grafana-url", SERVICE_TEST_GRAFANA_URL),
    )

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
