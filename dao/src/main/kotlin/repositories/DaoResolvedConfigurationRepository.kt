/*
 * Copyright (C) 2023 The ORT Project Authors (See <https://github.com/oss-review-toolkit/ort-server/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.server.dao.repositories

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert

import org.ossreviewtoolkit.server.dao.blockingQuery
import org.ossreviewtoolkit.server.dao.entityQuery
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.PackageCurationProviderConfigDao
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.ResolvedConfigurationDao
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.ResolvedConfigurationsIssueResolutionsTable
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.ResolvedConfigurationsPackageConfigurationsTable
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.ResolvedConfigurationsRuleViolationResolutionsTable
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.ResolvedConfigurationsTable
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.ResolvedConfigurationsVulnerabilityResolutionsTable
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.ResolvedPackageCurationDao
import org.ossreviewtoolkit.server.dao.tables.resolvedconfiguration.ResolvedPackageCurationProviderDao
import org.ossreviewtoolkit.server.dao.tables.runs.repository.IssueResolutionDao
import org.ossreviewtoolkit.server.dao.tables.runs.repository.PackageConfigurationDao
import org.ossreviewtoolkit.server.dao.tables.runs.repository.PackageCurationDao
import org.ossreviewtoolkit.server.dao.tables.runs.repository.PackageCurationDataDao
import org.ossreviewtoolkit.server.dao.tables.runs.repository.RuleViolationResolutionDao
import org.ossreviewtoolkit.server.dao.tables.runs.repository.VulnerabilityResolutionDao
import org.ossreviewtoolkit.server.dao.tables.runs.shared.IdentifierDao
import org.ossreviewtoolkit.server.model.repositories.ResolvedConfigurationRepository
import org.ossreviewtoolkit.server.model.resolvedconfiguration.ResolvedConfiguration
import org.ossreviewtoolkit.server.model.resolvedconfiguration.ResolvedPackageCurations
import org.ossreviewtoolkit.server.model.runs.repository.PackageConfiguration
import org.ossreviewtoolkit.server.model.runs.repository.Resolutions

class DaoResolvedConfigurationRepository(private val db: Database) : ResolvedConfigurationRepository {
    override fun get(id: Long): ResolvedConfiguration? = db.entityQuery { ResolvedConfigurationDao[id].mapToModel() }

    override fun getForOrtRun(ortRunId: Long): ResolvedConfiguration? = db.blockingQuery {
        ResolvedConfigurationDao.find { ResolvedConfigurationsTable.ortRunId eq ortRunId }.limit(1)
            .firstOrNull()?.mapToModel()
    }

    override fun addPackageConfigurations(ortRunId: Long, packageConfigurations: List<PackageConfiguration>) =
        db.blockingQuery {
            val resolvedConfiguration = ResolvedConfigurationDao.getOrPut(ortRunId)
            packageConfigurations.forEach { packageConfiguration ->
                val packageConfigurationDao = PackageConfigurationDao.getOrPut(packageConfiguration)
                ResolvedConfigurationsPackageConfigurationsTable.insert {
                    it[resolvedConfigurationId] = resolvedConfiguration.id
                    it[packageConfigurationId] = packageConfigurationDao.id
                }
            }
        }

    override fun addPackageCurations(ortRunId: Long, packageCurations: List<ResolvedPackageCurations>) =
        db.blockingQuery {
            val resolvedConfiguration = ResolvedConfigurationDao.getOrPut(ortRunId)
            val providerOffset = resolvedConfiguration.packageCurationProviders.count().toInt()
            packageCurations.forEachIndexed { index, resolvedPackageCurations ->
                val packageCurationProviderConfig =
                    PackageCurationProviderConfigDao.getOrPut(resolvedPackageCurations.provider)

                val resolvedPackageCurationProvider = ResolvedPackageCurationProviderDao.new {
                    this.packageCurationProviderConfig = packageCurationProviderConfig
                    this.resolvedConfiguration = resolvedConfiguration
                    this.rank = providerOffset + index
                }

                resolvedPackageCurations.curations.forEachIndexed { curationIndex, packageCuration ->
                    val packageCurationDao = PackageCurationDao.new {
                        identifier = IdentifierDao.getOrPut(packageCuration.id)
                        packageCurationData = PackageCurationDataDao.getOrPut(packageCuration.data)
                    }

                    ResolvedPackageCurationDao.new {
                        this.resolvedPackageCurationProvider = resolvedPackageCurationProvider
                        this.packageCuration = packageCurationDao
                        this.rank = curationIndex
                    }
                }
            }
        }

    override fun addResolutions(ortRunId: Long, resolutions: Resolutions) = db.blockingQuery {
        val resolvedConfiguration = ResolvedConfigurationDao.getOrPut(ortRunId)

        resolutions.issues.forEach { issueResolution ->
            val issueResolutionDao = IssueResolutionDao.getOrPut(issueResolution)
            ResolvedConfigurationsIssueResolutionsTable.insert {
                it[resolvedConfigurationId] = resolvedConfiguration.id
                it[issueResolutionId] = issueResolutionDao.id
            }
        }

        resolutions.ruleViolations.forEach { ruleViolationResolution ->
            val ruleViolationResolutionDao = RuleViolationResolutionDao.getOrPut(ruleViolationResolution)
            ResolvedConfigurationsRuleViolationResolutionsTable.insert {
                it[resolvedConfigurationId] = resolvedConfiguration.id
                it[ruleViolationResolutionId] = ruleViolationResolutionDao.id
            }
        }

        resolutions.vulnerabilities.forEach { vulnerabilityResolution ->
            val vulnerabilityResolutionDao = VulnerabilityResolutionDao.getOrPut(vulnerabilityResolution)
            ResolvedConfigurationsVulnerabilityResolutionsTable.insert {
                it[resolvedConfigurationId] = resolvedConfiguration.id
                it[vulnerabilityResolutionId] = vulnerabilityResolutionDao.id
            }
        }
    }
}