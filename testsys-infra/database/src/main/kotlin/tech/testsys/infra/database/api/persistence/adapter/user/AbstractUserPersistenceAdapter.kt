package tech.testsys.infra.database.api.persistence.adapter.user

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.UserAccessTokenFinder
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Base of adapters storing several user kinds in the shared [UserJpaEntity] table: [findById], [findByIds] and
 * [findByAccessToken] skip rows rejected by [supports], so a row of another kind is reported as absent rather than assembled.
 *
 * @param Data the data type a new entity is created from.
 * @param Id the id type of the entity.
 * @param Entity the domain entity type.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
abstract class AbstractUserPersistenceAdapter<Data, Id : UserId, Entity : User<Id>>(
    jpaEntityRepository: UserJpaEntityRepository,
) : AbstractPersistenceAdapter<Data, Id, Entity, UserJpaEntity>(jpaEntityRepository),
    UserAccessTokenFinder<Id, Entity> {

    private val userJpaEntityRepository: UserJpaEntityRepository = jpaEntityRepository

    @Transactional(readOnly = true)
    override fun findById(id: Id) = jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { supports(it) }?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<Id>) =
        jpaEntityRepository.findAllById(ids.map { it.value }).filter { supports(it) }.map { assemble(it) }

    @Transactional(readOnly = true)
    @RawAccessTokenDependency(
        reason = "Lookup by the stored value finds the user only while it equals the original access code (Identity).",
    )
    override fun findByAccessToken(rawAccessToken: String): Entity? {
        val accessTokenHash = AccessTokenHash.hashAccessToken(rawAccessToken, HashAlgorithm.Identity)
        return userJpaEntityRepository
            .findByAccessTokenAndAccessTokenHashAlgorithm(
                accessToken = accessTokenHash.value,
                accessTokenHashAlgorithm = accessTokenHash.algorithm.toJpaEnum(),
            )
            ?.takeIf { supports(it) }
            ?.let { assemble(it) }
    }

    /**
     * Whether [jpaEntity] holds a user of this adapter's kind.
     */
    protected abstract fun supports(jpaEntity: UserJpaEntity): Boolean
}
