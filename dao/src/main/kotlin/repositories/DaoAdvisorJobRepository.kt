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

package org.ossreviewtoolkit.server.dao.repositories

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

import org.ossreviewtoolkit.server.dao.blockingQuery
import org.ossreviewtoolkit.server.dao.tables.AdvisorJobDao
import org.ossreviewtoolkit.server.dao.tables.AdvisorJobsTable
import org.ossreviewtoolkit.server.dao.tables.OrtRunDao
import org.ossreviewtoolkit.server.model.AdvisorJob
import org.ossreviewtoolkit.server.model.AdvisorJobConfiguration
import org.ossreviewtoolkit.server.model.JobStatus
import org.ossreviewtoolkit.server.model.repositories.AdvisorJobRepository
import org.ossreviewtoolkit.server.model.util.OptionalValue

class DaoAdvisorJobRepository : AdvisorJobRepository {
    override fun create(ortRunId: Long, configuration: AdvisorJobConfiguration): AdvisorJob = blockingQuery {
        AdvisorJobDao.new {
            ortRun = OrtRunDao[ortRunId]
            createdAt = Clock.System.now()
            this.configuration = configuration
            status = JobStatus.CREATED
        }.mapToModel()
    }.getOrThrow()

    override fun get(id: Long) = blockingQuery { AdvisorJobDao[id].mapToModel() }.getOrNull()

    override fun getForOrtRun(ortRunId: Long): AdvisorJob? = blockingQuery {
        AdvisorJobDao.find { AdvisorJobsTable.ortRunId eq ortRunId }.limit(1).firstOrNull()?.mapToModel()
    }.getOrNull()

    override fun update(
        id: Long,
        startedAt: OptionalValue<Instant?>,
        finishedAt: OptionalValue<Instant?>,
        status: OptionalValue<JobStatus>
    ): AdvisorJob = blockingQuery {
        val advisorJob = AdvisorJobDao[id]

        startedAt.ifPresent { advisorJob.startedAt = it }
        finishedAt.ifPresent { advisorJob.finishedAt = it }
        status.ifPresent { advisorJob.status = it }

        AdvisorJobDao[id].mapToModel()
    }.getOrThrow()

    override fun delete(id: Long) = blockingQuery { AdvisorJobDao[id].delete() }.getOrThrow()
}