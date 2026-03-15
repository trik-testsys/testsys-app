package tech.testsys.infra.database.jpa.spring.component

import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment

/**
 * Physical naming strategy that derives table and column names from entity/field names.
 *
 * Tables: strips `JpaEntity`/`Entity` suffix, converts CamelCase to snake_case,
 * and prepends `t_` prefix. Example: `SubmissionStatusJpaEntity` → `t_submission_status`
 *
 * Columns: converts camelCase to snake_case. Example: `taskId` → `task_id`
 *
 * @author Roman Shishkin
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

        private const val TABLE_NAME_PREFIX = "t"

        private val CAMEL_CASE_REGEX = Regex("([a-z])([A-Z])")
        private const val SNAKE_CASE_REPLACEMENT = "$1_$2"
    }
}
