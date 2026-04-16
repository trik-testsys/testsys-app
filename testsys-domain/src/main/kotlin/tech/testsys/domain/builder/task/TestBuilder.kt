package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.VersionData

/**
 * Builder for constructing [TestData].
 *
 * @since %CURRENT_VERSION%
 */
class TestDataBuilder : Builder<TestData> {

    private var _file: FileData? = null
    private var _versionData: VersionData<TestId, Test>? = null

    /**
     * Sets the file data for the test.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Sets the version data for the test.
     *
     * @param root the ID of the root test in the version chain.
     * @param index the version index.
     * @since %CURRENT_VERSION%
     */
    fun versionData(root: TestId, index: Long) {
        _versionData = VersionData(root.lazify(), index)
    }

    /**
     * Builds the [TestData] instance.
     *
     * @return the constructed [TestData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): TestData {
        val file = requireField(_file) { ::_file }
        val versionData = requireField(_versionData) { ::_versionData }

        return TestData(
            file = file,
            versionData = versionData,
        )
    }

}

/**
 * Builder for constructing [Test] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class TestBuilder : DomainEntityWithDataBuilder<Test, TestData, TestDataBuilder>() {

    override fun dataBuilder() = TestDataBuilder()

    /**
     * Builds the [Test] instance.
     *
     * @return the constructed [Test].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Test {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Test(
            id = TestId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}
