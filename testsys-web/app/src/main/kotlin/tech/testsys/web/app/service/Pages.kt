package tech.testsys.web.app.service

import tech.testsys.domain.contract.persistence.Page

/** Transforms every element of the page, keeping its pagination. */
internal inline fun <T, R> Page<T>.map(transform: (T) -> R): Page<R> =
    Page(content = content.map(transform), pagination = pagination, totalElements = totalElements)
