package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import tech.testsys.infra.database.jpa.entity.JpaEntity

@MappedSuperclass
abstract class RoleEntity(
    @Column(name = "user_id")
    val userId: Long
) : JpaEntity()

@Entity
@Table(name = "t_developer_role")
class DeveloperRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

@Entity
@Table(name = "t_student_role")
class StudentRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

@Entity
@Table(name = "t_administrator_role")
class AdministratorRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

@Entity
@Table(name = "t_manager_role")
class ManagerRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)