package tech.testsys.infra.database.codegen.jpa

/** Qualified names of the JPA base types the processors look for. */
internal object JpaFqns {

    const val JPA_ENTITY = "jakarta.persistence.Entity"
    const val JPA_TRANSIENT = "jakarta.persistence.Transient"
    const val KOTLIN_ANY = "kotlin.Any"
    const val COMPOSITE_JPA_ENTITY = "tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity"
}
