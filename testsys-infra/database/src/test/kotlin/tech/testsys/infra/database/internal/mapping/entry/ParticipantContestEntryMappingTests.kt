package tech.testsys.infra.database.internal.mapping.entry

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTests

@InternalDatabaseApi
class ParticipantContestEntryMappingTests : EntityMappingTests<ParticipantContestEntryMapping>() {
    override val mapping = ParticipantContestEntryMapping
}
