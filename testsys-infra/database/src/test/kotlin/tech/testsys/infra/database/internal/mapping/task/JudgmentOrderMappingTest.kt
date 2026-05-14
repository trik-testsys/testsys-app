package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.infra.database.internal.mapping.EntityMappingTest
import tech.testsys.infra.database.internal.InternalDatabaseApi

@InternalDatabaseApi
class JudgmentOrderMappingTest : EntityMappingTest<JudgmentOrderMapping>() {

    override val mapping = JudgmentOrderMapping
}