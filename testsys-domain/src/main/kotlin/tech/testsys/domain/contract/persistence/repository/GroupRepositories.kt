package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId

/**
 * Persistence port for [Class] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ClassRepository : EntityRepository<ClassData, ClassId, Class>

/**
 * Persistence port for [Community] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface CommunityRepository : EntityRepository<CommunityData, CommunityId, Community>

/**
 * Persistence port for [Competition] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface CompetitionRepository : EntityRepository<CompetitionData, CompetitionId, Competition>
