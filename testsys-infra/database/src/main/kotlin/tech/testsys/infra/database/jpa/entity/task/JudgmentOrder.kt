package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import tech.testsys.infra.database.jpa.entity.JpaEntity


// TODO: видеозаписи, логи

@Entity
@Table(name = "t_judgment_order")
class JudgmentOrderJpaEntity(
    @Column(name = "judge_id")
    val judgeId: Long,

    @Column(name = "verdict_id")
    val verdictId: Long,
) : JpaEntity()
