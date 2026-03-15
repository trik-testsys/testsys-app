package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.user.MultipleRoleUserBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.UserId

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
    var owner: MultipleRoleUser? = null

    /**
     * The list of student user IDs enrolled in this class.
     *
     * @since %CURRENT_VERSION%
     */
    var students = mutableListOf<UserId>()

    /**
     * The list of contest IDs assigned to this class.
     *
     * @since %CURRENT_VERSION%
     */
    var contests = mutableListOf<ContestId>()

    /**
     * Configures the [owner] using a DSL block on [MultipleRoleUserBuilder].
     *
     * @param builder the configuration block for the owner.
     * @since %CURRENT_VERSION%
     */
    inline fun owner(builder: MultipleRoleUserBuilder.() -> Unit) {
        owner = MultipleRoleUserBuilder().apply(builder).build()
    }

    /**
     * Sets the [students] list from raw ID values.
     *
     * @param students the raw user IDs.
     * @since %CURRENT_VERSION%
     */
    fun students(students: Iterable<Long>) {
        this.students = students.map { UserId(it) }.toMutableList()
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
        val owner = requireField(owner, "owner")

        return ClassData(
            owner = owner,
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
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val data = requireField(data, "data")

        return Class(
            id = ClassId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}

/**
 * DSL entry point for building [ClassData].
 *
 * @param builder the configuration block applied to [ClassDataBuilder].
 * @return the constructed [ClassData].
 * @since %CURRENT_VERSION%
 */
inline fun buildClassData(builder: ClassDataBuilder.() -> Unit) = ClassDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Class].
 *
 * @param builder the configuration block applied to [ClassBuilder].
 * @return the constructed [Class].
 * @since %CURRENT_VERSION%
 */
inline fun buildClass(builder: ClassBuilder.() -> Unit) = ClassBuilder().apply(builder).build()
