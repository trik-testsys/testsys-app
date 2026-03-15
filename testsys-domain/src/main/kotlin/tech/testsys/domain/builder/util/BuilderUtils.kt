package tech.testsys.domain.builder.util

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.TrikSupportedLanguage

fun <Id : DomainId, Entity : DomainEntity<Id>> List<Id>.lazify() =
    LazyEntityList<Id, Entity>(this)

fun <Id : DomainId, Entity : DomainEntity<Id>> Id.lazify() =
    LazyEntity<Id, Entity>(this)
