package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData

class CommunityBuilderTests : DomainEntityBuilderTests<Community, CommunityData, CommunityDataBuilder>(
    CommunityBuilder(),
    CommunityDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(communityData {
        owner(42)
        name = "Community"
        description = "Community description"
    })
}
