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

package org.ossreviewtoolkit.server.dao.tables.runs.reporter

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

import org.ossreviewtoolkit.server.dao.tables.ReporterJobDao
import org.ossreviewtoolkit.server.dao.tables.ReporterJobsTable
import org.ossreviewtoolkit.server.dao.utils.toDatabasePrecision
import org.ossreviewtoolkit.server.model.runs.reporter.ReporterRun

/**
 * A table to represent a summary of a reporter run.
 */
object ReporterRunsTable : LongIdTable("reporter_runs") {
    val reporterJobId = reference("reporter_job_id", ReporterJobsTable)
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
}

class ReporterRunDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ReporterRunDao>(ReporterRunsTable)

    var reporterJob by ReporterJobDao referencedOn ReporterRunsTable.reporterJobId
    var startTime by ReporterRunsTable.startTime.transform({ it.toDatabasePrecision() }, { it })
    var endTime by ReporterRunsTable.endTime.transform({ it.toDatabasePrecision() }, { it })
    var reports by ReportDao via ReporterRunsReportsTable

    fun mapToModel() = ReporterRun(
        id = id.value,
        reporterJobId = reporterJob.id.value,
        startTime = startTime,
        endTime = endTime,
        reports = reports.map { it.mapToModel() }
    )
}