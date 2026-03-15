package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.user.MultipleRoleUserBuilder
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.UserId

class ClassDataBuilder : Builder<ClassData> {

    var owner: MultipleRoleUser? = null
    var students = mutableListOf<UserId>()
    var contests = mutableListOf<ContestId>()

    inline fun owner(builder: MultipleRoleUserBuilder.() -> Unit) {
        owner = MultipleRoleUserBuilder().apply(builder).build()
    }

    fun students(students: Iterable<Long>) {
        this.students = students.map { UserId(it) }.toMutableList()
    }

    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

    override fun build(): ClassData {
        val owner = requireNotNull(owner)

        return ClassData(
            owner = owner,
            students = students.lazify(),
            contests = contests.lazify(),
        )
    }

}

class ClassBuilder : DomainEntityWithDataBuilder<Class, ClassData, ClassDataBuilder>() {

    override fun dataBuilder() = ClassDataBuilder()

    override fun build(): Class {
        val id = requireNotNull(id)
        val createdAt = requireNotNull(createdAt)
        val data = requireNotNull(data)

        return Class(
            id = ClassId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}

inline fun buildClassData(builder: ClassDataBuilder.() -> Unit) = ClassDataBuilder().apply(builder).build()

inline fun buildClass(builder: ClassBuilder.() -> Unit) = ClassBuilder().apply(builder).build()
