package tech.testsys.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.infra.grpc.api.GradingNodeChangeResult
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.settings.api.StoredGradingNode
import tech.testsys.web.app.PostgresTestContainer

class GradingNodeRegistryRecoveryTests {
    @Test
    fun `should restore the stored registry instead of configured nodes after restarts`() {
        val database = PostgresTestContainer.instance
        val arguments = arguments("--testsys.grading.nodes=configured.invalid:50051")
        val active = GradingNodeAddress("active.invalid:50051")
        val inactive = GradingNodeAddress("inactive.invalid:50051")
        val jdbc = JdbcTemplate(DriverManagerDataSource(database.jdbcUrl, database.username, database.password))
        try {
            TestSysRuntime.start(arguments, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val registry = runtime.infrastructure.getBean(GradingNodeRegistry::class.java)
                assertEquals(emptyList<StoredGradingNode>(), registry.overview().nodes.map { entry -> entry.node })
                assertEquals(GradingNodeChangeResult.Applied, registry.add(active.target, isActive = true))
                assertEquals(GradingNodeChangeResult.Applied, registry.add(inactive.target, isActive = true))
                assertEquals(GradingNodeChangeResult.Applied, registry.setActive(inactive, isActive = false))
            }
            TestSysRuntime.start(arguments, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val registry = runtime.infrastructure.getBean(GradingNodeRegistry::class.java)
                assertEquals(setOf(active), runtime.infrastructure.getBean(BalancingGrader::class.java).getNodeStatuses().keys)
                assertEquals(
                    listOf(StoredGradingNode(active, isActive = true), StoredGradingNode(inactive, isActive = false)),
                    registry.overview().nodes.map { entry -> entry.node },
                )
                assertEquals(GradingNodeChangeResult.Applied, registry.remove(active))
                assertEquals(GradingNodeChangeResult.Applied, registry.remove(inactive))
            }
            TestSysRuntime.start(arguments, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val registry = runtime.infrastructure.getBean(GradingNodeRegistry::class.java)
                val grader = runtime.infrastructure.getBean(BalancingGrader::class.java)
                assertEquals(emptyList<StoredGradingNode>(), registry.overview().nodes.map { entry -> entry.node })
                assertEquals(emptyMap<GradingNodeAddress, Any>(), grader.getNodeStatuses())
            }
        } finally {
            jdbc.execute("truncate table ts_grading_node")
        }
    }

    @Test
    fun `should import configured nodes into the stored registry at startup with the import flag`() {
        val database = PostgresTestContainer.instance
        val nodes = "--testsys.grading.nodes=configured.invalid:50051,disabled.invalid:50051"
        val configured = GradingNodeAddress("configured.invalid:50051")
        val disabled = GradingNodeAddress("disabled.invalid:50051")
        val manual = GradingNodeAddress("manual.invalid:50051")
        val jdbc = JdbcTemplate(DriverManagerDataSource(database.jdbcUrl, database.username, database.password))
        try {
            TestSysRuntime.start(arguments(nodes), arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val registry = runtime.infrastructure.getBean(GradingNodeRegistry::class.java)
                assertEquals(GradingNodeChangeResult.Applied, registry.add(disabled.target, isActive = true))
                assertEquals(GradingNodeChangeResult.Applied, registry.setActive(disabled, isActive = false))
                assertEquals(GradingNodeChangeResult.Applied, registry.add(manual.target, isActive = false))
            }
            val importing = arguments(nodes, "--testsys.grading.import-nodes=true")
            TestSysRuntime.start(importing, arrayOf(RuntimeSettingsTestConfiguration::class.java)).use { runtime ->
                val registry = runtime.infrastructure.getBean(GradingNodeRegistry::class.java)
                val grader = runtime.infrastructure.getBean(BalancingGrader::class.java)
                assertEquals(
                    listOf(
                        StoredGradingNode(configured, isActive = true),
                        StoredGradingNode(disabled, isActive = true),
                        StoredGradingNode(manual, isActive = false),
                    ),
                    registry.overview().nodes.map { entry -> entry.node },
                )
                assertEquals(setOf(configured, disabled), grader.getNodeStatuses().keys)
            }
        } finally {
            jdbc.execute("truncate table ts_grading_node")
        }
    }

    private fun arguments(vararg extra: String): Array<String> {
        val database = PostgresTestContainer.instance
        return arrayOf(
            "--server.port=0",
            "--vaadin.productionMode=true",
            "--spring.datasource.url=${database.jdbcUrl}",
            "--spring.datasource.username=${database.username}",
            "--spring.datasource.password=${database.password}",
            "--testsys.service.secret=${"grading-recovery-".repeat(8)}",
            "--testsys.service.address=127.0.0.1",
            "--testsys.service.port=0",
            *extra,
        )
    }
}
