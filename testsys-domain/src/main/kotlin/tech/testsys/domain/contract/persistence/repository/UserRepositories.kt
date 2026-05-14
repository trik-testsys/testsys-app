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

interface MultipleRoleUserRepository : EntityRepository<MultipleRoleUserData, MultipleRoleUserId, MultipleRoleUser>

interface ObserverRepository : EntityRepository<ObserverData, SingleRoleUserId, Observer>

interface ParticipantRepository : EntityRepository<ParticipantData, SingleRoleUserId, Participant>

interface SupervisorRepository : EntityRepository<SupervisorData, SingleRoleUserId, Supervisor>
