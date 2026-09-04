package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.util.UUID

/**
 * JPA entity of [tech.testsys.domain.model.task.DeveloperSolution]: a [SolutionJpaEntity] plus the score it must reach;
 * the file lives on the solution row.
 *
 * @property name the name of the developer solution.
 * @property description the description of the developer solution.
 * @property versionBucket identity shared by all versions of the developer solution.
 * @property solutionId id of the [SolutionJpaEntity] holding the source.
 * @property expectedScore the score the solution is expected to reach.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class DeveloperSolutionJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val versionBucket: UUID,
    val solutionId: Long,
    val expectedScore: Int,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
