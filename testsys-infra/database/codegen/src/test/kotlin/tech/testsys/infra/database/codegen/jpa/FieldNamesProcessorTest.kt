package tech.testsys.infra.database.codegen.jpa

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
class FieldNamesProcessorTest {

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
                    .add(FieldNamesProcessorProvider())
            }
        }
        val result = compilation.compile()
        val generated = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        return Compiled(result, generated)
    }

    @Test
    fun `generates flat Fields object for SequenceJpaEntity subclass`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Entity
            import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

            @Entity
            class SampleJpaEntity(
                val name: String,
                val score: Int,
            ) : SequenceJpaEntity()
            """.trimIndent(),
        )

        val compiled = compile(entity)

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val generatedFile = compiled.generated.single { it.name == "SampleJpaEntity${'$'}Fields.kt" }
        val text = generatedFile.readText()

        assertTrue(text.contains("package sample"), text)
        assertTrue(text.contains("public object SampleJpaEntityFields"), text)
        // Inherited from JpaEntity:
        assertTrue(text.contains("""public const val CREATED_AT: String = "createdAt""""), text)
        assertTrue(text.contains("""public const val UPDATED_AT: String = "updatedAt""""), text)
        assertTrue(text.contains("""public const val VERSION: String = "version""""), text)
        // Inherited from SequenceJpaEntity:
        assertTrue(text.contains("""public const val ID: String = "id""""), text)
        // Declared on the entity:
        assertTrue(text.contains("""public const val NAME: String = "name""""), text)
        assertTrue(text.contains("""public const val SCORE: String = "score""""), text)
    }

    @Test
    fun `generates nested Id object for CompositeJpaEntity subclass`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Embeddable
            import jakarta.persistence.Entity
            import tech.testsys.infra.database.jpa.entity.CompositeId
            import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

            @Embeddable
            data class SampleId(val contestId: Long, val competitionId: Long) : CompositeId

            @Entity
            class SampleJpaEntity(id: SampleId) : CompositeJpaEntity<SampleId>(id)
            """.trimIndent(),
        )

        val compiled = compile(entity)

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val text = compiled.generated.single { it.name == "SampleJpaEntity${'$'}Fields.kt" }.readText()

        // Main object still contains inherited fields and ID:
        assertTrue(text.contains("public object SampleJpaEntityFields"), text)
        assertTrue(text.contains("""public const val ID: String = "id""""), text)
        assertTrue(text.contains("""public const val CREATED_AT: String = "createdAt""""), text)
        // Composite key fields are NOT duplicated at the top level:
        val topLevelObjectBody = text.substringAfter("public object SampleJpaEntityFields {")
            .substringBefore("public object Id")
        assertTrue(!topLevelObjectBody.contains("CONTEST_ID"), "CONTEST_ID must not appear at top level: $text")
        assertTrue(!topLevelObjectBody.contains("COMPETITION_ID"), "COMPETITION_ID must not appear at top level: $text")
        // Nested object Id contains composite key fields:
        assertTrue(text.contains("public object Id"), text)
        val nestedBody = text.substringAfter("public object Id")
        assertTrue(nestedBody.contains("""public const val CONTEST_ID: String = "contestId""""), text)
        assertTrue(nestedBody.contains("""public const val COMPETITION_ID: String = "competitionId""""), text)
    }

    @Test
    fun `excludes Transient annotated property`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Entity
            import jakarta.persistence.Transient
            import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

            @Entity
            class SampleJpaEntity(
                val keepMe: String,
                @Transient val skipMe: String,
            ) : SequenceJpaEntity()
            """.trimIndent(),
        )

        val compiled = compile(entity)

        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val text = compiled.generated.single { it.name == "SampleJpaEntity${'$'}Fields.kt" }.readText()
        assertTrue(text.contains("""public const val KEEP_ME: String = "keepMe""""), text)
        assertTrue(!text.contains("SKIP_ME"), "SKIP_ME must be excluded: $text")
        assertTrue(!text.contains("\"skipMe\""), "skipMe must be excluded: $text")
    }

    @Test
    fun `preserves declared property order in primary constructor`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Entity
            import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

            @Entity
            class SampleJpaEntity(
                val zebra: String,
                val apple: String,
                val mango: String,
            ) : SequenceJpaEntity()
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val text = compiled.generated.single { it.name == "SampleJpaEntity${'$'}Fields.kt" }.readText()
        val zIdx = text.indexOf("ZEBRA")
        val aIdx = text.indexOf("APPLE")
        val mIdx = text.indexOf("MANGO")
        assertTrue(zIdx in 0..<aIdx && aIdx < mIdx, "expected declared order ZEBRA<APPLE<MANGO; got z=$zIdx a=$aIdx m=$mIdx in:\n$text")
        // And inherited fields precede the declared ones:
        val createdIdx = text.indexOf("CREATED_AT")
        assertTrue(createdIdx in 0..<zIdx, "inherited CREATED_AT must precede declared ZEBRA in:\n$text")
    }

    @Test
    fun `generates Fields for plain Entity not extending project base classes`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Entity

            @Entity
            class StandaloneJpaEntity(val x: Int, val y: Int)
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val text = compiled.generated.single { it.name == "StandaloneJpaEntity${'$'}Fields.kt" }.readText()
        assertTrue(text.contains("public object StandaloneJpaEntityFields"), text)
        assertTrue(text.contains("""public const val X: String = "x""""), text)
        assertTrue(text.contains("""public const val Y: String = "y""""), text)
        // No inherited fields because there is no project base class:
        assertTrue(!text.contains("CREATED_AT"), "CREATED_AT must not appear: $text")
        assertTrue(!text.contains("\"id\""), "id must not appear: $text")
    }

    @Test
    fun `generates main object without nested Id when composite id lacks primary constructor`() {
        val entity = SourceFile.kotlin(
            "Sample.kt",
            """
            package sample
            import jakarta.persistence.Embeddable
            import jakarta.persistence.Entity
            import tech.testsys.infra.database.jpa.entity.CompositeId
            import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

            @Embeddable
            class WeirdId : CompositeId {
                constructor()
            }

            @Entity
            class SampleJpaEntity(id: WeirdId) : CompositeJpaEntity<WeirdId>(id)
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val text = compiled.generated.single { it.name == "SampleJpaEntity${'$'}Fields.kt" }.readText()
        assertTrue(text.contains("public object SampleJpaEntityFields"), text)
        assertTrue(text.contains("""public const val ID: String = "id""""), text)
        // No nested Id object:
        assertTrue(!text.contains("public object Id"), "no nested Id expected: $text")
        // Warning was logged:
        assertTrue(
            compiled.result.messages.contains("composite id WeirdId has no primary constructor"),
            "expected warning in messages: ${compiled.result.messages}",
        )
    }

    @Test
    fun `generates independent Fields objects for multiple entities`() {
        val entity = SourceFile.kotlin(
            "Multi.kt",
            """
            package sample
            import jakarta.persistence.Entity
            import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

            @Entity
            class FooJpaEntity(val foo: String) : SequenceJpaEntity()

            @Entity
            class BarJpaEntity(val bar: String) : SequenceJpaEntity()
            """.trimIndent(),
        )

        val compiled = compile(entity)
        assertEquals(KotlinCompilation.ExitCode.OK, compiled.result.exitCode, compiled.result.messages)
        val fooText = compiled.generated.single { it.name == "FooJpaEntity${'$'}Fields.kt" }.readText()
        val barText = compiled.generated.single { it.name == "BarJpaEntity${'$'}Fields.kt" }.readText()
        assertTrue(fooText.contains("public object FooJpaEntityFields"), fooText)
        assertTrue(fooText.contains("""public const val FOO: String = "foo""""), fooText)
        assertTrue(!fooText.contains("BAR"), "FooJpaEntity must not contain BAR: $fooText")
        assertTrue(barText.contains("public object BarJpaEntityFields"), barText)
        assertTrue(barText.contains("""public const val BAR: String = "bar""""), barText)
        assertTrue(!barText.contains("FOO"), "BarJpaEntity must not contain FOO: $barText")
    }

    companion object {
        private val STUBS = listOf(
            SourceFile.kotlin(
                "JakartaStubs.kt",
                """
                package jakarta.persistence
                @Target(AnnotationTarget.CLASS) annotation class Entity
                @Target(AnnotationTarget.CLASS) annotation class MappedSuperclass
                @Target(AnnotationTarget.CLASS) annotation class Embeddable
                @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY) annotation class Transient
                """.trimIndent(),
            ),
            SourceFile.kotlin(
                "BaseEntities.kt",
                """
                package tech.testsys.infra.database.jpa.entity
                import java.io.Serializable
                import jakarta.persistence.MappedSuperclass
                @MappedSuperclass
                abstract class JpaEntity(
                    val createdAt: Long = 0L,
                    val updatedAt: Long = 0L,
                    val version: Long = 0L,
                )
                interface CompositeId : Serializable
                @MappedSuperclass
                abstract class CompositeJpaEntity<T : CompositeId>(val id: T) : JpaEntity()
                @MappedSuperclass
                abstract class SequenceJpaEntity(val id: Long? = null) : JpaEntity()
                """.trimIndent(),
            ),
        )
    }
}
