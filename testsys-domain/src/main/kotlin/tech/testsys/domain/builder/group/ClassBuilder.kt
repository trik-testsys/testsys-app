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
 * Builder of [ClassData]. Required: [owner], [name], [description].
 *
 * @property owner the id of the owning manager, or `null` if not set yet.
 * @property name the name of the class, or `null` if not set yet.
 * @property description the description of the class, or `null` if not set yet.
 * @property students the ids of the enrolled students.
 * @property contests the ids of the assigned contests.
 * @since %CURRENT_VERSION%
 */
class ClassDataBuilder : Builder<ClassData> {

    var owner: MultipleRoleUserId? = null

    var name: String? = null

    var description: String? = null

    var students = mutableListOf<MultipleRoleUserId>()

    var contests = mutableListOf<ContestId>()

    /**
     * Sets [owner] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Sets [students] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun students(students: Iterable<Long>) {
        this.students = students.map { MultipleRoleUserId(it) }.toMutableList()
    }

    /**
     * Sets [contests] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

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
 * Builder of [Class] entities. Required: [id], [createdAt], [version], [data].
 *
 * @since %CURRENT_VERSION%
 */
class ClassBuilder : DomainEntityWithDataBuilder<Class, ClassData, ClassDataBuilder>() {

    override fun dataBuilder() = ClassDataBuilder()

    override fun build(): Class {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val version = requireField(version) { ::version }
        val data = requireField(data) { ::data }

        return Class(
            id = ClassId(id),
            createdAt = createdAt,
            version = version,
            data = data,
        )
    }
}
