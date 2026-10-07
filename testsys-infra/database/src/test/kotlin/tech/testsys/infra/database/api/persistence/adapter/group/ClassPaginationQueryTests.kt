package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.DatabaseIntegrationTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.group.ClassJpaEntityRepository
import java.time.Instant

@OptIn(InternalDatabaseApi::class)
class ClassPaginationQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: ClassRepository

    @Autowired
    private lateinit var jpaEntityRepository: ClassJpaEntityRepository

    @ParameterizedTest
    @ValueSource(strings = ["alpha", "ALPHA", "%", "_", "\\", "  "])
    fun `should match literal case insensitive substrings and preserve spaces`(substring: String) {
        val owner = fixtures.manager().id
        val expected = saveClass(ownerId = owner, name = "  Alpha%_\\Beta  ")
        saveClass(ownerId = owner, name = "Other plain name")
        saveClass(ownerId = fixtures.manager().id, name = "  Alpha%_\\Beta  ")

        val page = repository.findAvailableToManager(
            ownerId = owner,
            pagination = Pagination(page = 0, size = 1),
            filter = ClassFilter(name = substring),
        )

        assertEquals(listOf(expected.id), page.content.map { entity -> entity.id })
        assertEquals(1L, page.totalElements)
    }

    @ParameterizedTest
    @CsvSource("0,2,true", "1,1,false", "2,0,false")
    fun `should exclude foreign classes before paging and counting`(index: Int, size: Int, hasNext: Boolean) {
        val owner = fixtures.manager().id
        val otherOwner = fixtures.manager().id
        saveClass(ownerId = otherOwner, name = "Alpha")
        val first = saveClass(ownerId = owner, name = "Alpha")
        val second = saveClass(ownerId = owner, name = "Alpha")
        val third = saveClass(ownerId = owner, name = "Alpha")
        saveClass(ownerId = owner, name = "Excluded")
        saveClass(ownerId = otherOwner, name = "Alpha")
        val expected = listOf(first.id, second.id, third.id).drop(index * 2).take(2)
        val request = Pagination(page = index, size = 2)

        val page =
            repository.findAvailableToManager(ownerId = owner, pagination = request, filter = ClassFilter(name = "Alpha"))

        assertEquals(expected, page.content.map { entity -> entity.id })
        assertEquals(size, page.content.size)
        assertEquals(3L, page.totalElements)
        assertEquals(2, page.totalPages)
        assertEquals(hasNext, page.hasNext)
        assertSame(request, page.pagination)
        assertTrue(page.content.all { entity -> entity.data.owner.id == owner })
    }

    @Test
    fun `should assemble class details and student count without changing membership`() {
        val owner = fixtures.manager().id
        val students = listOf(fixtures.student().id, fixtures.student().id)
        val contest = fixtures.contest().id
        val saved = repository.save(
            classData {
                this.owner = owner
                name = "Viewed class"
                description = "Class description"
                this.students = students.toMutableList()
                contests = mutableListOf(contest)
            },
        )

        val page = repository.findAvailableToManager(ownerId = owner, pagination = Pagination(page = 0, size = 1))

        val actual = page.content.single()
        assertEquals(saved.id, actual.id)
        assertEquals("Viewed class", actual.data.name)
        assertEquals("Class description", actual.data.description)
        assertEquals(owner, actual.data.owner.id)
        assertEquals(students.toSet(), actual.data.students.ids.toSet())
        assertEquals(2, actual.data.students.ids.size)
        assertEquals(listOf(contest), actual.data.contests.ids)
        assertEquals(saved.version, actual.version)
        assertEquals(students.toSet(), repository.findById(saved.id)?.data?.students?.ids?.toSet())
    }

    @ParameterizedTest
    @CsvSource("20,,middle|last", ",20,first|middle", "20,30,middle|last", "20,20,middle")
    fun `should include each creation bound and leave unspecified bounds unrestricted`(from: Long?, to: Long?, expected: String) {
        val owner = fixtures.manager().id
        val saved = mapOf(
            "first" to saveClass(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(10)),
            "middle" to saveClass(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(20)),
            "last" to saveClass(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(30)),
        )
        saveClass(ownerId = fixtures.manager().id, name = "Alpha", createdAt = Instant.ofEpochSecond(20))
        val filter = ClassFilter(
            createdFrom = from?.let { Instant.ofEpochSecond(it) },
            createdTo = to?.let { Instant.ofEpochSecond(it) },
        )

        val page =
            repository.findAvailableToManager(ownerId = owner, pagination = Pagination(page = 0, size = 10), filter = filter)

        assertEquals(expected.split("|").map { key -> saved.getValue(key).id }, page.content.map { entity -> entity.id })
        assertEquals(expected.split("|").size.toLong(), page.totalElements)
    }

    @Test
    fun `should combine name and creation filters before paging and counting`() {
        val owner = fixtures.manager().id
        saveClass(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(9))
        saveClass(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(10))
        val expected = saveClass(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(20))
        saveClass(ownerId = owner, name = "Beta", createdAt = Instant.ofEpochSecond(15))
        saveClass(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(21))
        saveClass(ownerId = fixtures.manager().id, name = "Alpha", createdAt = Instant.ofEpochSecond(15))
        val filter =
            ClassFilter(name = "ALPHA", createdFrom = Instant.ofEpochSecond(10), createdTo = Instant.ofEpochSecond(20))

        val page =
            repository.findAvailableToManager(ownerId = owner, pagination = Pagination(page = 1, size = 1), filter = filter)

        assertEquals(listOf(expected.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertFalse(page.hasNext)
    }

    @ParameterizedTest
    @CsvSource("0,first", "1,second")
    fun `should break equal custom sort values by ascending id across pages`(index: Int, expected: String) {
        val owner = fixtures.manager().id
        val first = saveClass(ownerId = owner, name = "Equal")
        val second = saveClass(ownerId = owner, name = "Equal")
        val saved = mapOf("first" to first.id, "second" to second.id)
        val request =
            Pagination(page = index, size = 1, sort = Sort(listOf(Sort.Order(field = "name", direction = Sort.Direction.DESC))))

        val page = repository.findAvailableToManager(ownerId = owner, pagination = request)

        assertEquals(listOf(saved.getValue(expected)), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "createdAt", "name"])
    fun `should preserve explicitly descending ordering`(field: String) {
        val owner = fixtures.manager().id
        saveClass(ownerId = owner, name = "First", createdAt = Instant.ofEpochSecond(10))
        val last = saveClass(ownerId = owner, name = "Last", createdAt = Instant.ofEpochSecond(20))
        val request =
            Pagination(page = 0, size = 1, sort = Sort(listOf(Sort.Order(field = field, direction = Sort.Direction.DESC))))

        val page = repository.findAvailableToManager(ownerId = owner, pagination = request)

        assertEquals(listOf(last.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should match all owned names with an empty substring`() {
        val owner = fixtures.manager().id
        val first = saveClass(ownerId = owner, name = "First")
        val second = saveClass(ownerId = owner, name = "Second")
        saveClass(ownerId = fixtures.manager().id, name = "Foreign")

        val page = repository.findAvailableToManager(
            ownerId = owner,
            pagination = Pagination(page = 0, size = 2),
            filter = ClassFilter(name = ""),
        )

        assertEquals(listOf(first.id, second.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertFalse(page.hasNext)
    }

    @ParameterizedTest
    @ValueSource(strings = ["absent", "Foreign"])
    fun `should return an empty page if no owned class matches`(name: String) {
        val owner = fixtures.manager().id
        saveClass(ownerId = owner, name = "First")
        saveClass(ownerId = fixtures.manager().id, name = "Foreign")

        val page = repository.findAvailableToManager(
            ownerId = owner,
            pagination = Pagination(page = 0, size = 1),
            filter = ClassFilter(name = name),
        )

        assertEquals(emptyList<Class>(), page.content)
        assertEquals(0L, page.totalElements)
        assertEquals(0, page.totalPages)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should return an empty page when manager owns no classes`() {
        val owner = fixtures.manager().id
        saveClass(ownerId = fixtures.manager().id, name = "Foreign")

        val page = repository.findAvailableToManager(ownerId = owner, pagination = Pagination(page = 0, size = 1))

        assertEquals(emptyList<Class>(), page.content)
        assertEquals(0L, page.totalElements)
    }

    private fun saveClass(ownerId: MultipleRoleUserId, name: String, createdAt: Instant = Instant.EPOCH): Class {
        val saved = repository.save(
            classData {
                owner = ownerId
                this.name = name
                description = "Pagination query test"
            },
        )
        setCreatedAt(id = saved.id, createdAt = createdAt)
        return saved
    }

    private fun setCreatedAt(id: ClassId, createdAt: Instant) {
        val entity = jpaEntityRepository.findById(id.value).orElseThrow()
        entity.createdAt = createdAt
        jpaEntityRepository.saveAndFlush(entity)
    }
}
