package tech.testsys.domain.model

import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId

interface Describable {

    val name: String

    val description: String?
}

interface Sharable {

    val sharedTo: LazyEntityList<CommunityId, Community>
}