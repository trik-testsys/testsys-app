package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData

class MultipleRoleUserBuilderTests : DomainEntityBuilderTests<MultipleRoleUser, MultipleRoleUserData, MultipleRoleUserDataBuilder>(
    MultipleRoleUserBuilder(),
    MultipleRoleUserDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(
        multipleRoleUserData {
            accessToken = "token"
            name = "Alice"
            email = "alice@example.com"
            roles {
                developer {
                    memberOf(listOf(1))
                    data = developerData {}
                }
            }
        },
        multipleRoleUserData {
            accessToken = "token"
            name = "Bob"
            email = "bob@example.com"
            roles {
                student {
                    memberOf(listOf(1))
                    data = studentData {
                        classes(listOf(1L, 2L))
                        submissions(listOf(10L))
                    }
                }
            }
        },
        multipleRoleUserData {
            accessToken = "token"
            name = "Carol"
            email = "carol@example.com"
            roles {
                judge {
                    memberOf(listOf(1))
                    data = judgeData {
                        judgmentOrders(listOf(1L, 2L))
                    }
                }
            }
        },
        multipleRoleUserData {
            accessToken = "token"
            name = "Dan"
            email = "dan@example.com"
            roles {
                manager {
                    memberOf(listOf(1))
                    data = managerData {
                        classes(listOf(1L))
                        competitions(listOf(10L))
                    }
                }
            }
        },
        multipleRoleUserData {
            accessToken = "token"
            name = "Eve"
            email = "eve@example.com"
            roles {
                administrator {}
            }
        },
        multipleRoleUserData {
            accessToken = "token"
            name = "Frank"
            email = "frank@example.com"
            roles {
                developer {
                    memberOf(listOf(1))
                    data = developerData {
                        tasks(listOf(1L))
                        contests(listOf(2L))
                    }
                }
                student {
                    data = studentData {}
                }
                administrator {}
            }
        },
    )
}
