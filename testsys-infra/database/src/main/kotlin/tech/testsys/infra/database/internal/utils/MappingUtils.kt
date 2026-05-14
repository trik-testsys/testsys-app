package tech.testsys.infra.database.internal.utils

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.JpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikSupportedLanguageEnum

@InternalDatabaseApi
internal fun <Entity : SequenceJpaEntity> Entity.requireId() = requireNotNull(id) { "${this::class.simpleName} has null id" }

@InternalDatabaseApi
internal fun <Entity : JpaEntity> Entity?.requireById(id: Any) = requireNotNull(this) { "Entity not found by id=$id" }

@InternalDatabaseApi
internal fun <Entity : DomainEntity<Id>, Id : DomainId> Entity?.requireById(id: Id) = requireNotNull(this) { "Domain entity not found by id=$id" }

@InternalDatabaseApi
internal fun <Entity, Data, DataBuilder : Builder<Data>> DomainEntityWithDataBuilder<Entity, Data, DataBuilder>.populateFields(
    jpaEntity: SequenceJpaEntity,
) {
    id = jpaEntity.requireId()
    createdAt = jpaEntity.createdAt
}

@InternalDatabaseApi
internal fun LanguageChooser.chose(language: TrikSupportedLanguageEnum) = when (language) {
    TrikSupportedLanguageEnum.PYTHON -> python()
    TrikSupportedLanguageEnum.JAVA_SCRIPT -> javaScript()
    TrikSupportedLanguageEnum.VISUAL_LANGUAGE -> visualLanguage()
}

@InternalDatabaseApi
internal fun TrikSupportedLanguage.toJpaEnum() = when (this) {
    TrikSupportedLanguage.Python -> TrikSupportedLanguageEnum.PYTHON
    TrikSupportedLanguage.JavaScript -> TrikSupportedLanguageEnum.JAVA_SCRIPT
    TrikSupportedLanguage.VisualLanguage -> TrikSupportedLanguageEnum.VISUAL_LANGUAGE
}