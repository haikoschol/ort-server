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

package org.ossreviewtoolkit.server.dao.tables.runs.scanner

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

import org.ossreviewtoolkit.server.model.runs.scanner.ScannerConfiguration

/**
 * A table to represent a scanner configuration.
 */
object ScannerConfigurationsTable : LongIdTable("scanner_configurations") {
    val scannerRunId = reference("scanner_run_id", ScannerRunsTable)

    val skipConcluded = bool("skip_concluded").default(false)
    val createMissingArchives = bool("create_missing_archives").default(false)
    val storageReaders = text("storage_readers").nullable()
    val storageWriters = text("storage_writers").nullable()
    val ignorePatterns = text("ignore_patterns").nullable()
}

class ScannerConfigurationDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ScannerConfigurationDao>(ScannerConfigurationsTable)

    var scannerRun by ScannerRunDao referencedOn ScannerConfigurationsTable.scannerRunId
    val fileArchiveConfiguration by FileArchiverConfigurationDao optionalBackReferencedOn
            FileArchiverConfigurationsTable.scannerConfigurationId
    val provenanceStorageConfiguration by ProvenanceStorageConfigurationDao optionalBackReferencedOn
            ProvenanceStorageConfigurationsTable.scannerConfigurationId

    var skipConcluded by ScannerConfigurationsTable.skipConcluded
    var createMissingArchives by ScannerConfigurationsTable.createMissingArchives
    var storageReaders: List<String>? by ScannerConfigurationsTable.storageReaders
        .transform({ it?.joinToString(",") }, { it?.split(",") })
    var storageWriters: List<String>? by ScannerConfigurationsTable.storageWriters
        .transform({ it?.joinToString(",") }, { it?.split(",") })
    var ignorePatterns: List<String>? by ScannerConfigurationsTable.ignorePatterns
        .transform({ it?.joinToString(",") }, { it?.split(",") })
    var detectedLicenseMappings by DetectedLicenseMappingDao via
            ScannerConfigurationsDetectedLicenseMappingsTable
    val options by ScannerConfigurationScannerOptionDao referrersOn
            ScannerConfigurationsScannerOptionsTable.scannerConfigurationId
    val storages by ScannerConfigurationStorageDao referrersOn
            ScannerConfigurationsStoragesTable.scannerConfigurationId

    fun mapToModel() = ScannerConfiguration(
        skipConcluded = skipConcluded,
        archive = fileArchiveConfiguration?.mapToModel(),
        createMissingArchives = createMissingArchives,
        detectedLicenseMappings = detectedLicenseMappings.associate { it.license to it.spdxLicense },
        options = options.associate { scannerOptions ->
            scannerOptions.scanner to scannerOptions.options.associate { it.key to it.value }
        },
        storages = storages.associate { it.storage to it.storages.mapToModel() },
        storageReaders = storageReaders,
        storageWriters = storageWriters,
        ignorePatterns = ignorePatterns.orEmpty(),
        provenanceStorage = provenanceStorageConfiguration?.mapToModel()
    )
}