package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTest

@InternalDatabaseApi
class RecordingMappingTest : EntityMappingTest<RecordingMapping>() {

    override val mapping = RecordingMapping
}