package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import java.util.UUID

/**
 * Builder for constructing [StatementData].
 *
 * @since %CURRENT_VERSION%
 */
class StatementDataBuilder : Builder<StatementData> {

    private var _file: FileData? = null

    /**
     * The name of the statement.
     *
     * @since %CURRENT_VERSION%
     */
    var name: String? = null

    /**
     * The description of the statement.
     *
     * @since %CURRENT_VERSION%
     */
    var description: String? = null

    /**
     * Logical identity shared by all versions of this statement.
     *
     * @since %CURRENT_VERSION%
     */
    var versionBucket: UUID? = null

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
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val versionBucket = requireField(versionBucket) { ::versionBucket }

        return StatementData(
            file = file,
            name = name,
            description = description,
            versionBucket = versionBucket,
        )
    }
}

/**
 * Builder for constructing [Statement] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class StatementBuilder : DomainEntityWithDataBuilder<Statement, StatementData, StatementDataBuilder>() {

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
        val data = requireField(data) { ::data }

        return Statement(
            id = StatementId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}
