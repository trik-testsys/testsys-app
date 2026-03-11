package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "t_supervisor_role")
class SupervisorRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

@Entity
@Table(name = "t_participant_role")
class ParticipantRoleJpaEntity(
    userId: Long,
    @Column(name = "competition_id")
    val competitionId: Long
) : RoleEntity(userId)

@Entity
@Table(name = "t_manager_role")
class ObserverRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)
