package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData

class ClassBuilderTests : DomainEntityBuilderTests<Class, ClassData, ClassDataBuilder>(
    ClassBuilder(),
    ClassDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(
        classData { owner(42) },
        classData {
            owner(42)
            students(listOf(1L, 2L))
            contests(listOf(10L, 20L))
        },
    )
}