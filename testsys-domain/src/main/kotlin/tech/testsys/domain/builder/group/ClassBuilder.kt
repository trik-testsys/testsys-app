package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Builder for constructing [ClassData].
 *
 * @since %CURRENT_VERSION%
 */
class ClassDataBuilder : Builder<ClassData> {


    /**
     * The owner of the class.
     *
     * @since %CURRENT_VERSION%
     */
    var owner: MultipleRoleUserId? = null

    var name: String? = null

    var description: String? = null

    /**
     * The list of student user IDs enrolled in this class.
     *
     * @since %CURRENT_VERSION%
     */
    var students = mutableListOf<MultipleRoleUserId>()

    /**
     * The list of contest IDs assigned to this class.
     *
     * @since %CURRENT_VERSION%
     */
    var contests = mutableListOf<ContestId>()

    /**
     * Sets the [owner] from a raw ID value.
     *
     * @param owner the raw owner ID.
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Sets the [students] list from raw ID values.
     *
     * @param students the raw user IDs.
     * @since %CURRENT_VERSION%
     */
    fun students(students: Iterable<Long>) {
        this.students = students.map { MultipleRoleUserId(it) }.toMutableList()
    }

    /**
     * Sets the [contests] list from raw ID values.
     *
     * @param contests the raw contest IDs.
     * @since %CURRENT_VERSION%
     */
    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

    /**
     * Builds the [ClassData] instance.
     *
     * @return the constructed [ClassData].
     * @throws IllegalArgumentException if [owner] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): ClassData {
        val owner = requireField(owner) { ::owner }
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }

        return ClassData(
            owner = owner.lazify(),
            name = name,
            description = description,
            students = students.lazify(),
            contests = contests.lazify(),
        )
    }

}

/**
 * Builder for constructing [Class] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class ClassBuilder : DomainEntityWithDataBuilder<Class, ClassData, ClassDataBuilder>() {

    override fun dataBuilder() = ClassDataBuilder()

    /**
     * Builds the [Class] instance.
     *
     * @return the constructed [Class].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Class {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Class(
            id = ClassId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}
