package tech.testsys.infra.database.internal.jpa.entity.settings

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * Technical record of the checking node registry, independent of domain aggregates.
 *
 * @property address the unique gRPC target of the node.
 * @property isActive whether the node participates in grading.
 * @since 1.0.0-beta.1
 */
@Entity
@Table(uniqueConstraints = [UniqueConstraint(name = "uk_ts_grading_node_address", columnNames = ["address"])])
@InternalDatabaseApi
class GradingNodeJpaEntity(
    val address: String,
    var isActive: Boolean,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
