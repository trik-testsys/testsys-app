package tech.testsys.infra.database.api.service

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.utils.toDomain

/**
 * Service listing and registration of TRIK Studio versions over [TrikStudioVersionJpaEntity] rows. A concurrent
 * registration of the same value violates a unique constraint, and the repeated transaction refuses the value as
 * already registered.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DatabaseServiceTrikStudioVersions(
    private val contestRepository: ContestRepository,
    private val trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository,
) : ServiceTrikStudioVersions {

    @Transactional(readOnly = true)
    override fun findAll(): List<TrikStudioVersion> = contestRepository.findTrikStudioVersions()

    @Transactional
    override fun register(tag: String, version: String): TrikStudioVersionRegistration {
        val trimmedTag = tag.trim()
        val trimmedVersion = version.trim()

        val rejection = rejectionOf(tag = trimmedTag, version = trimmedVersion)
        if (rejection != null) return TrikStudioVersionRegistration.Rejected(rejection)

        val saved = trikStudioVersionJpaEntityRepository.save(
            TrikStudioVersionJpaEntity(tag = trimmedTag, studioVersion = trimmedVersion),
        )
        return TrikStudioVersionRegistration.Registered(saved.toDomain())
    }

    private fun rejectionOf(tag: String, version: String): TrikStudioVersionRejection? = when {
        version.isEmpty() -> TrikStudioVersionRejection.BlankVersion
        version.codePointCount(0, version.length) > MAX_VERSION_LENGTH -> TrikStudioVersionRejection.VersionTooLong
        trikStudioVersionJpaEntityRepository.existsByStudioVersion(version) -> TrikStudioVersionRejection.VersionAlreadyRegistered
        tag.isEmpty() -> TrikStudioVersionRejection.BlankTag
        tag.codePointCount(0, tag.length) > MAX_TAG_LENGTH -> TrikStudioVersionRejection.TagTooLong
        trikStudioVersionJpaEntityRepository.findByTag(tag) != null -> TrikStudioVersionRejection.TagAlreadyRegistered
        else -> null
    }

    private companion object {

        /** Length of the `studio_version` column. */
        const val MAX_VERSION_LENGTH = 255

        /** Length of the `tag` column. */
        const val MAX_TAG_LENGTH = 512
    }
}
