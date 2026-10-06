@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.api

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.support.ResourcePropertySource
import tech.testsys.infra.grpc.internal.InternalGrpcApi

internal class GrpcConfigurationTests {
    private val configuration = GrpcConfiguration()
    private val environment = StandardEnvironment().apply {
        propertySources.addFirst(ResourcePropertySource("classpath:grading-defaults.properties"))
    }

    @Test
    fun `should enable video recording from the module defaults`() {
        val settings = configuration.gradingSettings(environment)

        assertTrue(settings.shouldRecordVideo)
    }

    @Test
    fun `should disable video recording when the environment overrides its default`() {
        environment.propertySources.addFirst(MapPropertySource("override", mapOf("testsys.grading.should-record-video" to "false")))

        val settings = configuration.gradingSettings(environment)

        assertFalse(settings.shouldRecordVideo)
    }
}
