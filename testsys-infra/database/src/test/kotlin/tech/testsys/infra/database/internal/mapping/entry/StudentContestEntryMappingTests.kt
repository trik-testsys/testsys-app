package tech.testsys.infra.database.internal.mapping.entry

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTests

@InternalDatabaseApi
class StudentContestEntryMappingTests : EntityMappingTests<StudentContestEntryMapping>() {
    override val mapping = StudentContestEntryMapping
}
