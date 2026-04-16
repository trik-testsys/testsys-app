package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.VersionData

/**
 * Builder for constructing [StatementData].
 *
 * @since %CURRENT_VERSION%
 */
class StatementDataBuilder : Builder<StatementData> {

    private var _file: FileData? = null

    /**
     * Sets the file data for the statement.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Builds the [StatementData] instance.
     *
     * @return the constructed [StatementData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): StatementData {
        val file = requireField(_file) { ::_file }

        return StatementData(
            file = file,
        )
    }

}

/**
 * Builder for constructing [Statement] domain entities.
 * Exercises support versioning via [versionData].
 *
 * @since %CURRENT_VERSION%
 */
class StatementBuilder : DomainEntityWithDataBuilder<Statement, StatementData, StatementDataBuilder>() {

    private var _versionData: VersionData<StatementId, Statement>? = null

    /**
     * Sets the version data for the statement.
     *
     * @param root the ID of the root statement in the version chain.
     * @param index the version index.
     * @since %CURRENT_VERSION%
     */
    fun versionData(root: StatementId, index: Long) {
        _versionData = VersionData(root.lazify(), index)
    }

    override fun dataBuilder() = StatementDataBuilder()

    /**
     * Builds the [Statement] instance.
     *
     * @return the constructed [Statement].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Statement {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val versionData = requireField(_versionData) { ::_versionData }
        val data = requireField(data) { ::data }

        return Statement(
            id = StatementId(id),
            createdAt = createdAt,
            versionData = versionData,
            data = data,
        )
    }

}
