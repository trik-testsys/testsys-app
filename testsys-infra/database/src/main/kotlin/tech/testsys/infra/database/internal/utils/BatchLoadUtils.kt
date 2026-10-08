package tech.testsys.infra.database.internal.utils

import tech.testsys.infra.database.internal.InternalDatabaseApi

// A power of two, matching `in_clause_parameter_padding`, so every full chunk reuses one cached statement.
private const val IN_CLAUSE_CHUNK_SIZE = 1024

/**
 * Passes the distinct [ids] to [find] in chunks of at most [chunkSize] ids and concatenates the found rows in chunk
 * order; empty [ids] return an empty list without calling [find].
 */
@InternalDatabaseApi
internal fun <Key, Row> findAllInChunks(
    ids: Collection<Key>,
    chunkSize: Int = IN_CLAUSE_CHUNK_SIZE,
    find: (List<Key>) -> List<Row>,
): List<Row> {
    require(chunkSize > 0) { "IN clause chunk size must be positive, got $chunkSize" }
    return ids.distinct().chunked(chunkSize).flatMap(find)
}

/**
 * Reads join rows of all [ownerIds] through [findAllInChunks] and groups [linkedIdOf] of each row by [ownerIdOf]
 * in the order of the found rows; every requested owner is a key, with an empty list if it has no rows.
 */
@InternalDatabaseApi
internal fun <Key, Row, LinkedId> findLinkedIds(
    ownerIds: Collection<Key>,
    find: (List<Key>) -> List<Row>,
    ownerIdOf: (Row) -> Key,
    linkedIdOf: (Row) -> LinkedId,
    chunkSize: Int = IN_CLAUSE_CHUNK_SIZE,
): Map<Key, List<LinkedId>> {
    val linkedIdsByOwner = findAllInChunks(ids = ownerIds, chunkSize = chunkSize, find = find)
        .groupBy(keySelector = ownerIdOf, valueTransform = linkedIdOf)
    return ownerIds.distinct().associateWith { ownerId -> linkedIdsByOwner[ownerId].orEmpty() }
}
