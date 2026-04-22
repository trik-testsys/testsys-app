package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class DescribableJpaEntity(
    val name: String,
    val description: String,
) : SequenceJpaEntity()