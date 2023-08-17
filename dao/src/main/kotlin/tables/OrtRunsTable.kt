/*
 * Copyright (C) 2022 The ORT Project Authors (See <https://github.com/oss-review-toolkit/ort-server/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.server.dao.tables

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

import org.ossreviewtoolkit.server.dao.tables.runs.repository.RepositoryConfigurationDao
import org.ossreviewtoolkit.server.dao.tables.runs.repository.RepositoryConfigurationsTable
import org.ossreviewtoolkit.server.dao.tables.runs.shared.OrtIssueDao
import org.ossreviewtoolkit.server.dao.tables.runs.shared.VcsInfoTable
import org.ossreviewtoolkit.server.dao.utils.SortableEntityClass
import org.ossreviewtoolkit.server.dao.utils.SortableTable
import org.ossreviewtoolkit.server.dao.utils.jsonb
import org.ossreviewtoolkit.server.dao.utils.toDatabasePrecision
import org.ossreviewtoolkit.server.model.JobConfigurations
import org.ossreviewtoolkit.server.model.OrtRun
import org.ossreviewtoolkit.server.model.OrtRunStatus

/**
 * A table to represent an ORT run.
 */
object OrtRunsTable : SortableTable("ort_runs") {
    val repositoryId = reference("repository_id", RepositoriesTable)

    val index = long("index").sortable()
    val revision = text("revision").sortable()
    val createdAt = timestamp("created_at").sortable("createdAt")

    // TODO: Create a proper database representation for configurations, JSON is only used because of the expected
    //       frequent changes during early development.
    val jobConfigs = jsonb("job_configs", JobConfigurations::class)
    val resolvedJobConfigs = jsonb("resolved_job_configs", JobConfigurations::class).nullable()
    val jobConfigContext = text("job_config_context").nullable()
    val resolvedJobConfigContext = text("resolved_job_config_context").nullable()
    val vcsId = reference("vcs_id", VcsInfoTable).nullable()
    val vcsProcessedId = reference("vcs_processed_id", VcsInfoTable).nullable()
    val status = enumerationByName<OrtRunStatus>("status", 128)
}

class OrtRunDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : SortableEntityClass<OrtRunDao>(OrtRunsTable)

    var repository by RepositoryDao referencedOn OrtRunsTable.repositoryId

    var index by OrtRunsTable.index
    var revision by OrtRunsTable.revision
    var createdAt by OrtRunsTable.createdAt.transform({ it.toDatabasePrecision() }, { it })
    var jobConfigs by OrtRunsTable.jobConfigs
    var resolvedJobConfigs by OrtRunsTable.resolvedJobConfigs
    var jobConfigContext by OrtRunsTable.jobConfigContext
    var resolvedJobConfigContext by OrtRunsTable.resolvedJobConfigContext
    var status by OrtRunsTable.status
    var issues by OrtIssueDao via OrtRunsIssuesTable
    var labels by LabelDao via OrtRunsLabelsTable
    var vcsId by OrtRunsTable.vcsId
    var vcsProcessedId by OrtRunsTable.vcsProcessedId

    val advisorJob by AdvisorJobDao optionalBackReferencedOn AdvisorJobsTable.ortRunId
    val analyzerJob by AnalyzerJobDao optionalBackReferencedOn AnalyzerJobsTable.ortRunId
    val evaluatorJob by EvaluatorJobDao optionalBackReferencedOn EvaluatorJobsTable.ortRunId
    val scannerJob by ScannerJobDao optionalBackReferencedOn ScannerJobsTable.ortRunId
    val reporterJob by ReporterJobDao optionalBackReferencedOn ReporterJobsTable.ortRunId
    val repositoryConfig by RepositoryConfigurationDao optionalBackReferencedOn RepositoryConfigurationsTable.ortRunId
    val nestedRepositories by NestedRepositoryDao referrersOn NestedRepositoriesTable.ortRunId

    fun mapToModel() = OrtRun(
        id = id.value,
        index = index,
        repositoryId = repository.id.value,
        revision = revision,
        createdAt = createdAt,
        jobConfigs = jobConfigs,
        resolvedJobConfigs = resolvedJobConfigs,
        status = status,
        labels = labels.associate { it.mapToModel() },
        vcsId = vcsId?.value,
        vcsProcessedId = vcsProcessedId?.value,
        nestedRepositoryIds = nestedRepositories.associate { it.path to it.vcsId.value },
        repositoryConfigId = repositoryConfig?.id?.value,
        issues = issues.map { it.mapToModel() },
        jobConfigContext = jobConfigContext,
        resolvedJobConfigContext = resolvedJobConfigContext
    )
}
