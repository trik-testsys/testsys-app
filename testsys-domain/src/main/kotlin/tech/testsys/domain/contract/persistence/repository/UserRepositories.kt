package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData

/**
 * Persistence port for [MultipleRoleUser] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface MultipleRoleUserRepository : EntityRepository<MultipleRoleUserData, MultipleRoleUserId, MultipleRoleUser>

/**
 * Persistence port for [Observer] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ObserverRepository : EntityRepository<ObserverData, SingleRoleUserId, Observer>

/**
 * Persistence port for [Participant] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ParticipantRepository : EntityRepository<ParticipantData, SingleRoleUserId, Participant>

/**
 * Persistence port for [Supervisor] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface SupervisorRepository : EntityRepository<SupervisorData, SingleRoleUserId, Supervisor>
