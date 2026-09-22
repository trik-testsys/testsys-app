package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.statement
import tech.testsys.domain.builder.api.statementData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class StatementPersistenceAdapterTests : PersistenceAdapterContractTests<StatementData, StatementId, Statement>() {

    @Autowired
    override lateinit var repository: StatementRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    override fun newData() = statementData {
        name = fixtures.unique("Statement")
        description = "Statement description"
        file(fixtures.unique("statement") + ".pdf", "statement".toByteArray())
        versionBucket = UUID.randomUUID()
    }

    override fun modified(entity: Statement) = entity.withData {
        name = fixtures.unique("Renamed statement")
        description = "Updated description"
        file(fixtures.unique("statement") + ".pdf", "changed statement".toByteArray())
    }

    override fun detached(entity: Statement) = statement {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = StatementId(value)

    override fun assertSameData(expected: Statement, actual: Statement) {
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.file.uploadedFilename, actual.data.file.uploadedFilename)
        assertContentEquals(expected.data.file.content, actual.data.file.content)
        assertEquals(expected.data.versionBucket, actual.data.versionBucket)
    }

    @Test
    fun `should store a new file version on update with a renamed file`() {
        val saved = repository.save(newData())

        repository.update(saved.withData { file("renamed.pdf", saved.data.file.content) })

        assertEquals("renamed.pdf", assertNotNull(repository.findById(saved.id)).data.file.uploadedFilename)
        assertEquals(2, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should keep the stored file on update with an unchanged file`() {
        val saved = repository.save(newData())

        repository.update(saved.withData { name = fixtures.unique("Renamed statement") })

        assertEquals(1, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should keep the version bucket if another bucket is passed on update`() {
        val saved = repository.save(newData())

        val updated = repository.update(saved.withData { versionBucket = UUID.randomUUID() })

        assertEquals(saved.data.versionBucket, updated.data.versionBucket)
        assertEquals(saved.data.versionBucket, assertNotNull(repository.findById(saved.id)).data.versionBucket)
    }
}
