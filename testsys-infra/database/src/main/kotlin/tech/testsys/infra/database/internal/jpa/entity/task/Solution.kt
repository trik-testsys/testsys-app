package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity of [tech.testsys.domain.model.task.Solution].
 *
 * @property fileDataId id of the [FileDataJpaEntity] holding the source.
 * @property language programming language of the source.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class SolutionJpaEntity(
    val fileDataId: Long,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageEnum,
    id: Long? = null,
) : SequenceJpaEntity(id)
