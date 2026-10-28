package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [FileDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface FileDataJpaEntityRepository : SnowflakeJpaEntityRepository<FileDataJpaEntity> {

    /**
     * Finds unowned file metadata ids in ascending order within [pageable], regardless of creation time.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select candidate.id from FileDataJpaEntity candidate " +
            "where not exists (select 1 from StatementJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from ExerciseJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from TestJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from SolutionJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from LogsJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from RecordingJpaEntity owner where owner.fileDataId = candidate.id) " +
            "order by candidate.id",
    )
    fun findOrphanIds(pageable: Pageable): List<Long>

    /**
     * Deletes file metadata from [ids] still without owners, returning the deleted count.
     *
     * @since %CURRENT_VERSION%
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "delete from FileDataJpaEntity candidate where candidate.id in :ids " +
            "and not exists (select 1 from StatementJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from ExerciseJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from TestJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from SolutionJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from LogsJpaEntity owner where owner.fileDataId = candidate.id) " +
            "and not exists (select 1 from RecordingJpaEntity owner where owner.fileDataId = candidate.id) ",
    )
    fun deleteOrphansByIds(@Param("ids") ids: Collection<Long>): Int

    /**
     * Finds stored blob keys present in [keys] without loading file metadata entities.
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e.storedFileName from FileDataJpaEntity e where e.storedFileName in :keys")
    fun findStoredKeys(@Param("keys") keys: Collection<String>): List<String>
}

/**
 * Spring Data repository for [TrikStudioVersionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TrikStudioVersionJpaEntityRepository : SnowflakeJpaEntityRepository<TrikStudioVersionJpaEntity> {

    /**
     * Finds the version with [tag], or `null` if it is not stored.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByTag(tag: String): TrikStudioVersionJpaEntity?

    /**
     * Finds the stored versions with any of [tags] in one query.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByTagIn(tags: Collection<String>): List<TrikStudioVersionJpaEntity>

    /**
     * Checks whether a version with the version number [studioVersion] is stored.
     *
     * @since %CURRENT_VERSION%
     */
    fun existsByStudioVersion(studioVersion: String): Boolean

    /**
     * Finds the tags of the versions referenced by a contest, a submission, a task revision or a task validation
     * request.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        nativeQuery = true,
        value = """
            select v.tag from ts_trik_studio_version v
            where exists (select 1 from ts_contest c where c.trik_studio_version_id = v.id)
               or exists (select 1 from ts_submission s where s.trik_studio_version_id = v.id)
               or exists (select 1 from ts_trik_studio_version_to_task_content t where t.trik_studio_version_id = v.id)
               or exists (
                   select 1 from ts_trik_studio_version_to_task_validation_request r where r.trik_studio_version_id = v.id
               )
        """,
    )
    fun findUsedTags(): List<String>
}
