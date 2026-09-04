package tech.testsys.infra.database.internal.jpa

import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment

/**
 * Hibernate naming strategy: snake_case columns (`taskId` → `task_id`); tables also drop the `JpaEntity`/`Entity`
 * suffix and get the `ts_` prefix (`SubmissionJpaEntity` → `ts_submission`).
 *
 * @since %CURRENT_VERSION%
 */
class TestsysPhysicalNamingStrategy : PhysicalNamingStrategyStandardImpl() {

    override fun toPhysicalTableName(logicalName: Identifier, jdbcEnvironment: JdbcEnvironment): Identifier {
        val name = logicalName.text
            .removeSuffix(JPA_ENTITY_SUFFIX)
            .removeSuffix(ENTITY_SUFFIX)
            .replace(CAMEL_CASE_REGEX, SNAKE_CASE_REPLACEMENT)
            .lowercase()
        return Identifier.toIdentifier("${TABLE_NAME_PREFIX}_$name")
    }

    override fun toPhysicalColumnName(logicalName: Identifier, jdbcEnvironment: JdbcEnvironment): Identifier {
        val name = logicalName.text
            .replace(CAMEL_CASE_REGEX, SNAKE_CASE_REPLACEMENT)
            .lowercase()
        return Identifier.toIdentifier(name)
    }

    companion object {

        private const val JPA_ENTITY_SUFFIX = "JpaEntity"
        private const val ENTITY_SUFFIX = "Entity"

        private const val TABLE_NAME_PREFIX = "ts"

        private val CAMEL_CASE_REGEX = Regex("([a-z])([A-Z])")
        private const val SNAKE_CASE_REPLACEMENT = "$1_$2"
    }
}
