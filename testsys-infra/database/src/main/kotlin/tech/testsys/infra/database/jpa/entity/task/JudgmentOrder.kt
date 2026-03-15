package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaEntity


// TODO: видеозаписи, логи

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class JudgmentOrderJpaEntity(
    val judgeId: Long,
    val verdictId: Long,
) : JpaEntity()
