package tech.testsys.infra.database.jpa.beans.component

import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import org.springframework.stereotype.Component

/**
 * Physical naming strategy that derives database object names from entity/field names.
 *
 * Tables: strips `JpaEntity`/`Entity` suffix, converts CamelCase to snake_case,
 * and prepends the `ts_` prefix. Example: `SubmissionJpaEntity` → `ts_submission`.
 *
 * Columns: converts camelCase to snake_case. Example: `taskId` → `task_id`.
 *
 * Sequences: strips the `_SEQ` suffix produced by Hibernate, then applies the same
 * normalization as tables and re-appends `_seq`. Example: Hibernate's logical
 * `SubmissionJpaEntity_SEQ` → `ts_submission_seq`.
 *
 * @since %CURRENT_VERSION%
 */
@Component
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

    override fun toPhysicalSequenceName(logicalName: Identifier, jdbcEnvironment: JdbcEnvironment): Identifier {
        val name = logicalName.text
            .removeSuffix(SEQUENCE_SUFFIX)
            .removeSuffix(JPA_ENTITY_SUFFIX)
            .removeSuffix(ENTITY_SUFFIX)
            .replace(CAMEL_CASE_REGEX, SNAKE_CASE_REPLACEMENT)
            .lowercase()
            .removeSuffix("_seq")
            .removePrefix("${TABLE_NAME_PREFIX}_")
        return Identifier.toIdentifier("${TABLE_NAME_PREFIX}_${name}_seq")
    }

    companion object {

        private const val JPA_ENTITY_SUFFIX = "JpaEntity"
        private const val ENTITY_SUFFIX = "Entity"
        private const val SEQUENCE_SUFFIX = "_SEQ"

        private const val TABLE_NAME_PREFIX = "ts"

        private val CAMEL_CASE_REGEX = Regex("([a-z])([A-Z])")
        private const val SNAKE_CASE_REPLACEMENT = "$1_$2"
    }
}
