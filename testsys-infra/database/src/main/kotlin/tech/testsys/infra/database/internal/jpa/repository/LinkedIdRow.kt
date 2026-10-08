package tech.testsys.infra.database.internal.jpa.repository

import tech.testsys.infra.database.internal.InternalDatabaseApi

/**
 * Pair of ids returned by a projection query instead of whole rows when only the ids of related rows are needed.
 *
 * @property ownerId the id of the row the query selects by.
 * @property linkedId the id of the related row.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
data class LinkedIdRow(val ownerId: Long, val linkedId: Long)
