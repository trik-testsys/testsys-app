package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.ClassInviteId
import java.time.Instant

/**
 * Builder of [ClassInviteData]. Required: [code] or [storedCode], [expiresAt].
 *
 * @property expiresAt the moment the invite stops being valid, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class ClassInviteDataBuilder : InviteCodeDataBuilder<ClassInviteData>() {

    var expiresAt: Instant? = null

    override fun build(): ClassInviteData {
        val codeHash = requireCodeHash()
        val expiresAt = requireField(expiresAt) { ::expiresAt }

        return ClassInviteData(
            codeHash = codeHash,
            expiresAt = expiresAt,
        )
    }
}

/**
 * Builder of [ClassInvite] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class ClassInviteBuilder : DomainEntityWithDataBuilder<ClassInvite, ClassInviteData, ClassInviteDataBuilder>() {

    override fun dataBuilder() = ClassInviteDataBuilder()

    override fun build(): ClassInvite {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return ClassInvite(
            id = ClassInviteId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
