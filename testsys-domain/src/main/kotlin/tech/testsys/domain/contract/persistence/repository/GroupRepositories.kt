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

interface ClassRepository : EntityRepository<ClassData, ClassId, Class>

interface CommunityRepository : EntityRepository<CommunityData, CommunityId, Community>

interface CompetitionRepository : EntityRepository<CompetitionData, CompetitionId, Competition>
