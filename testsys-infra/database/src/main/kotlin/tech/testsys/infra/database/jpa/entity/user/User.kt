package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import tech.testsys.infra.database.jpa.entity.JpaEntity

@Entity
@Table(name = "t_users")
class UserJpaEntity(
    @Column(name = "access_token")
    val accessToken: String,
) : JpaEntity()