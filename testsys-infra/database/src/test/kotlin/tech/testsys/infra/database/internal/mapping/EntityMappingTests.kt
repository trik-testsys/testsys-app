package tech.testsys.infra.database.internal.mapping

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.infra.database.internal.InternalDatabaseApi
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.memberFunctions
import kotlin.test.assertEquals

@InternalDatabaseApi
abstract class EntityMappingTests<M : EntityMapping<*, *>> {

    protected abstract val mapping: M

    protected val domainKClass: KClass<*> by lazy { entityMappingTypeArguments().first }
    protected val jpaKClass: KClass<*> by lazy { entityMappingTypeArguments().second }

    @Nested
    inner class ToDomainMethodTests {

        @Test
        fun `should declare a toDomain method`() {
            val containsToDomainMethod = mapping::class.memberFunctions
                .any { it.name == TO_DOMAIN_METHOD_NAME }

            assertTrue(containsToDomainMethod)
        }

        @Test
        fun `should return a Domain-typed object from toDomain`() {
            val toDomainMethod = mapping::class.memberFunctions
                .first { it.name == TO_DOMAIN_METHOD_NAME }

            assertEquals(domainKClass, toDomainMethod.returnType.classifier)
        }
    }

    @Nested
    inner class ToJpaEntityMethodTests {

        @Test
        fun `should declare a toJpaEntity method`() {
            val containsToJpaEntityMethod = mapping::class.memberFunctions
                .any { it.name == TO_JPA_ENTITY_METHOD_NAME }

            assertTrue(containsToJpaEntityMethod)
        }

        @Test
        fun `should return a Jpa-typed object from every toJpaEntity overload`() {
            val overloads = mapping::class.memberFunctions
                .filter { it.name == TO_JPA_ENTITY_METHOD_NAME }
                .toList()

            assertTrue(overloads.isNotEmpty(), "no toJpaEntity overloads found on ${mapping::class.simpleName}")
            overloads.forEach { overload ->
                assertEquals(
                    jpaKClass,
                    overload.returnType.classifier,
                    "overload $overload returns ${overload.returnType} instead of $jpaKClass",
                )
            }
        }
    }

    private fun entityMappingTypeArguments(): Pair<KClass<*>, KClass<*>> {
        val entityMappingType = mapping::class.allSupertypes
            .firstOrNull { it.classifier == EntityMapping::class }
            ?: error("${mapping::class.simpleName} does not declare EntityMapping<Domain, Jpa> in its supertypes")

        val args = entityMappingType.arguments
        require(args.size == EXPECTED_ENTITY_MAPPING_TYPE_ARGUMENT_COUNT) {
            "EntityMapping should have exactly $EXPECTED_ENTITY_MAPPING_TYPE_ARGUMENT_COUNT type arguments, got ${args.size}"
        }
        val domain = args[0].type?.classifier as? KClass<*>
            ?: error("Domain type argument of EntityMapping on ${mapping::class.simpleName} is not a class")
        val jpa = args[1].type?.classifier as? KClass<*>
            ?: error("Jpa type argument of EntityMapping on ${mapping::class.simpleName} is not a class")
        return domain to jpa
    }

    companion object {

        private const val TO_DOMAIN_METHOD_NAME = "toDomain"
        private const val TO_JPA_ENTITY_METHOD_NAME = "toJpaEntity"
        private const val EXPECTED_ENTITY_MAPPING_TYPE_ARGUMENT_COUNT = 2
    }
}
