package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.VersionBucket

/**
 * Builder of [TestData]. Required: [file], [name], [description], [versionBucket].
 *
 * @property name the name of the test, or `null` if not set yet.
 * @property description the description of the test, or `null` if not set yet.
 * @property versionBucket the version chain shared by all versions of the test, or `null` if not set yet.
 * @since 1.0.0-beta.1
 */
class TestDataBuilder : Builder<TestData> {

    private var _file: FileData? = null

    var name: String? = null

    var description: String? = null

    var versionBucket: VersionBucket? = null

    /**
     * Sets the file.
     *
     * @param uploadedFilename the original name of the uploaded file.
     * @param content the raw binary content of the file.
     * @since 1.0.0-beta.1
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Sets [file] without reading its contents.
     *
     * @since 1.0.0-beta.1
     */
    fun file(file: FileData) {
        _file = file
    }

    override fun build(): TestData {
        val file = requireField(_file) { ::_file }
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val versionBucket = requireField(versionBucket) { ::versionBucket }

        return TestData(
            file = file,
            name = name,
            description = description,
            versionBucket = versionBucket,
        )
    }
}

/**
 * Builder of [Test] entities. Required: [id], [createdAt], [data].
 *
 * @since 1.0.0-beta.1
 */
class TestBuilder : DomainEntityWithDataBuilder<Test, TestData, TestDataBuilder>() {

    override fun dataBuilder() = TestDataBuilder()

    override fun build(): Test {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Test(
            id = TestId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
