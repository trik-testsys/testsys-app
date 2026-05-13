package tech.testsys.infra.codegen.jpa

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
@Suppress("FunctionNaming")
class CompositeKeyConstructorProcessorTest {

    private data class Compiled(
        val result: JvmCompilationResult,
        val generated: List<File>,
    )

    private fun compile(vararg sources: SourceFile): Compiled {
        val compilation = KotlinCompilation().apply {
            this.sources = sources.toList() + STUBS
            inheritClassPath = true
            messageOutputStream = System.out
            configureKsp {
                @Suppress("UNCHECKED_CAST")
                (symbolProcessorProviders as MutableList<SymbolProcessorProvider>)
                    .add(CompositeKeyConstructorProcessorProvider())
            }
        }
        val result = compilation.compile()
        val generated = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        return Compiled(result, generated)
    }

    @Test
    fun `generates factory function for canonical 2-Long id`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Embeddable
            import jakarta.persistence.Entity
            import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
            import tech.testsys.infra.database.jpa.entity.CompositeId
            import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

            @Embeddable
            data class SampleId(val aId: Long, val bId: Long) : CompositeId

            @CompositeKeyConstructor
            @Entity
            class SampleJpaEntity(id: SampleId) : CompositeJpaEntity<SampleId>(id)
            """.trimIndent(),
        )

        val compiled = compile(entity)

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val generatedFile = compiled.generated.single { it.name == "SampleJpaEntity${'$'}Constructors.kt" }
        val text = generatedFile.readText()
        assertTrue(text.contains("package sample"), text)
        assertTrue(text.contains("public fun SampleJpaEntity("), text)
        assertTrue(text.contains("aId: kotlin.Long") || text.contains("aId: Long"), text)
        assertTrue(text.contains("bId: kotlin.Long") || text.contains("bId: Long"), text)
        assertTrue(text.contains("SampleId(") && text.contains("aId = aId") && text.contains("bId = bId"), text)
    }

    @Test
    fun `generates factory for id with enum and Long fields`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Embeddable
            import jakarta.persistence.Entity
            import jakarta.persistence.EnumType
            import jakarta.persistence.Enumerated
            import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
            import tech.testsys.infra.database.jpa.entity.CompositeId
            import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

            enum class Role { ADMIN, USER }

            @Embeddable
            data class SampleId(
                @Enumerated(EnumType.STRING) val role: Role,
                val userId: Long,
                val tenantId: Long,
            ) : CompositeId

            @CompositeKeyConstructor
            @Entity
            class SampleJpaEntity(id: SampleId) : CompositeJpaEntity<SampleId>(id)
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val text = compiled.generated.single { it.name == "SampleJpaEntity${'$'}Constructors.kt" }.readText()
        assertTrue(text.contains("role: sample.Role") || text.contains("role: Role"), text)
        assertTrue(text.contains("userId: kotlin.Long") || text.contains("userId: Long"), text)
        // The @Enumerated annotation must NOT propagate to the generated parameter.
        assertTrue(!text.contains("@Enumerated"), "generated factory must not carry JPA annotations: $text")
    }

    @Test
    fun `preserves parameter order from id primary constructor`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Embeddable
            import jakarta.persistence.Entity
            import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
            import tech.testsys.infra.database.jpa.entity.CompositeId
            import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

            @Embeddable
            data class SampleId(val zId: Long, val aId: Long) : CompositeId

            @CompositeKeyConstructor
            @Entity
            class SampleJpaEntity(id: SampleId) : CompositeJpaEntity<SampleId>(id)
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val text = compiled.generated.single { it.name == "SampleJpaEntity${'$'}Constructors.kt" }.readText()
        val zIdx = text.indexOf("zId:")
        val aIdx = text.indexOf("aId:")
        assertTrue(zIdx >= 0 && aIdx >= 0, "both params present: $text")
        assertTrue(zIdx < aIdx, "zId must precede aId in generated signature; got order=[zId@$zIdx, aId@$aIdx]")
    }

    @Test
    fun `fails when annotated class does not extend CompositeJpaEntity`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor

            @CompositeKeyConstructor
            class NotAnEntity
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(
            compiled.result.messages.contains("must extend CompositeJpaEntity"),
            "expected error message: ${compiled.result.messages}",
        )
    }

    @Test
    fun `fails when entity has constructor params beyond id`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Embeddable
            import jakarta.persistence.Entity
            import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
            import tech.testsys.infra.database.jpa.entity.CompositeId
            import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

            @Embeddable
            data class SampleId(val aId: Long, val bId: Long) : CompositeId

            @CompositeKeyConstructor
            @Entity
            class SampleJpaEntity(
                id: SampleId,
                val extra: String,
            ) : CompositeJpaEntity<SampleId>(id)
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(
            compiled.result.messages.contains("single primary constructor parameter named 'id'"),
            compiled.result.messages,
        )
    }

    @Test
    fun `fails when id type is not a data class`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Embeddable
            import jakarta.persistence.Entity
            import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
            import tech.testsys.infra.database.jpa.entity.CompositeId
            import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

            @Embeddable
            class SampleId(val aId: Long, val bId: Long) : CompositeId

            @CompositeKeyConstructor
            @Entity
            class SampleJpaEntity(id: SampleId) : CompositeJpaEntity<SampleId>(id)
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(
            compiled.result.messages.contains("must be a Kotlin `data class`"),
            compiled.result.messages,
        )
    }

    @Test
    fun `fails when id constructor has a non-val parameter`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Embeddable
            import jakarta.persistence.Entity
            import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
            import tech.testsys.infra.database.jpa.entity.CompositeId
            import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

            @Embeddable
            data class SampleId(val aId: Long, var bId: Long) : CompositeId

            @CompositeKeyConstructor
            @Entity
            class SampleJpaEntity(id: SampleId) : CompositeJpaEntity<SampleId>(id)
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compiled.result.exitCode)
        assertTrue(
            compiled.result.messages.contains("must be declared `val`"),
            compiled.result.messages,
        )
    }

    companion object {
        private val STUBS = listOf(
            SourceFile.kotlin(
                "Stubs.kt",
                """
                package tech.testsys.infra.database.jpa.entity
                import java.io.Serializable
                interface CompositeId : Serializable
                abstract class CompositeJpaEntity<T : CompositeId>(val id: T)
                """.trimIndent(),
            ),
            SourceFile.kotlin(
                "JakartaStubs.kt",
                """
                package jakarta.persistence
                @Target(AnnotationTarget.CLASS) annotation class Embeddable
                @Target(AnnotationTarget.CLASS) annotation class Entity
                enum class EnumType { ORDINAL, STRING }
                @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
                annotation class Enumerated(val value: EnumType = EnumType.ORDINAL)
                """.trimIndent(),
            ),
        )
    }
}
