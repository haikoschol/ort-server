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

package org.ossreviewtoolkit.server.workers.scanner

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

import org.ossreviewtoolkit.model.ArtifactProvenance
import org.ossreviewtoolkit.model.Hash
import org.ossreviewtoolkit.model.HashAlgorithm
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.Provenance
import org.ossreviewtoolkit.model.RemoteArtifact
import org.ossreviewtoolkit.model.RepositoryProvenance
import org.ossreviewtoolkit.model.UnknownProvenance
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.VcsType
import org.ossreviewtoolkit.scanner.provenance.ResolvedArtifactProvenance
import org.ossreviewtoolkit.scanner.provenance.ResolvedRepositoryProvenance
import org.ossreviewtoolkit.scanner.provenance.UnresolvedPackageProvenance
import org.ossreviewtoolkit.server.dao.blockingQuery
import org.ossreviewtoolkit.server.dao.tables.runs.scanner.ScannerRunDao
import org.ossreviewtoolkit.server.dao.test.DatabaseTestExtension
import org.ossreviewtoolkit.server.model.runs.scanner.ScannerRun
import org.ossreviewtoolkit.server.workers.common.mapToOrt

class OrtServerPackageProvenanceStorageTest : WordSpec() {
    private val dbExtension = extension(DatabaseTestExtension())

    private lateinit var packageProvenanceStorage: OrtServerPackageProvenanceStorage
    private lateinit var scannerRun: ScannerRun

    init {
        beforeEach {
            scannerRun = dbExtension.fixtures.scannerRunRepository.create(dbExtension.fixtures.scannerJob.id)
            packageProvenanceStorage = OrtServerPackageProvenanceStorage(dbExtension.db, scannerRun.id)
        }

        /**
         * Verify that the provided [provenance] was associated to the [scannerRun]. Must be called before calling
         * any other functions on [packageProvenanceStorage] which might also create the association.
         */
        fun verifyAssociatedProvenance(scannerRun: ScannerRun, provenance: Provenance) {
            dbExtension.db.blockingQuery {
                val associatedProvenances =
                    ScannerRunDao[scannerRun.id].packageProvenances.map { it.mapToModel().mapToOrt() }
                associatedProvenances should haveSize(1)
                associatedProvenances.single() shouldBe provenance
            }
        }

        "putProvenance" should {
            "create an artifact provenance in the database and associate it to the scanner run" {
                val id = createIdentifier()
                val sourceArtifact = createRemoteArtifact()
                val provenance = createArtifactProvenance(sourceArtifact)

                packageProvenanceStorage.putProvenance(id, sourceArtifact, provenance)

                verifyAssociatedProvenance(scannerRun, provenance.provenance)
                packageProvenanceStorage.readProvenance(id, sourceArtifact) shouldBe provenance
            }

            "create a repository provenance in the database and associate it to the scanner run" {
                val id = createIdentifier()
                val vcsInfo = createVcsInfo()
                val provenance = createRepositoryProvenance(vcsInfo)

                packageProvenanceStorage.putProvenance(id, vcsInfo, provenance)

                verifyAssociatedProvenance(scannerRun, provenance.provenance)
                packageProvenanceStorage.readProvenance(id, vcsInfo) shouldBe provenance
            }

            "create an unresolved provenance in the database and associate it to the scanner run" {
                val id = createIdentifier()
                val vcsInfo = createVcsInfo()
                val provenance = UnresolvedPackageProvenance("message")

                packageProvenanceStorage.putProvenance(id, vcsInfo, provenance)

                verifyAssociatedProvenance(scannerRun, UnknownProvenance)
                packageProvenanceStorage.readProvenance(id, vcsInfo) shouldBe provenance
            }

            "be able to store multiple entries for the same id and provenance" {
                val id = createIdentifier()
                val vcsInfo = createVcsInfo()
                val provenance = createRepositoryProvenance(vcsInfo)
                val errorProvenance = createErrorProvenance("Provenance error")

                packageProvenanceStorage.putProvenance(id, vcsInfo, provenance)
                packageProvenanceStorage.putProvenance(id, vcsInfo, errorProvenance)

                packageProvenanceStorage.readProvenances(id) should
                        containExactlyInAnyOrder(provenance, errorProvenance)
            }
        }

        "readProvenance" should {
            fun createScannerRun(): ScannerRun {
                val ortRun = dbExtension.fixtures.createOrtRun()
                val scannerJob = dbExtension.fixtures.createScannerJob(ortRun.id)
                return dbExtension.fixtures.scannerRunRepository.create(scannerJob.id)
            }

            "return null if no result for an artifact provenance is stored" {
                val id = createIdentifier()
                val sourceArtifact = createRemoteArtifact()

                packageProvenanceStorage.readProvenance(id, sourceArtifact) should beNull()
            }

            "return the latest result for an artifact provenance and associate it with the scanner run" {
                val id = createIdentifier()
                val sourceArtifact = createRemoteArtifact()
                val artifactProvenance = createArtifactProvenance(sourceArtifact)
                packageProvenanceStorage.putProvenance(id, sourceArtifact, UnresolvedPackageProvenance("message"))
                packageProvenanceStorage.putProvenance(id, sourceArtifact, artifactProvenance)

                // Create a new scanner run and related storage as the put call above already associated the provenance
                // with the default scanner run.
                val newScannerRun = createScannerRun()
                val newStorage = OrtServerPackageProvenanceStorage(dbExtension.db, newScannerRun.id)
                val result = newStorage.readProvenance(id, sourceArtifact)

                result.shouldBeInstanceOf<ResolvedArtifactProvenance>()
                result shouldBe artifactProvenance

                verifyAssociatedProvenance(newScannerRun, artifactProvenance.provenance)
            }

            "return null if no result for a repository provenance is stored" {
                val id = createIdentifier()
                val vcsInfo = createVcsInfo()

                packageProvenanceStorage.readProvenance(id, vcsInfo) should beNull()
            }

            "return the latest result for a repository provenance and associate it with the scanner run" {
                val id = createIdentifier()
                val vcsInfo = createVcsInfo()
                val repositoryProvenance = createRepositoryProvenance(vcsInfo)
                packageProvenanceStorage.putProvenance(id, vcsInfo, UnresolvedPackageProvenance("message"))
                packageProvenanceStorage.putProvenance(id, vcsInfo, repositoryProvenance)

                // Create a new scanner run and related storage as the put call above already associated the provenance
                // with the default scanner run.
                val newScannerRun = createScannerRun()
                val newStorage = OrtServerPackageProvenanceStorage(dbExtension.db, newScannerRun.id)
                val result = newStorage.readProvenance(id, vcsInfo)

                result.shouldBeInstanceOf<ResolvedRepositoryProvenance>()
                result shouldBe repositoryProvenance

                verifyAssociatedProvenance(newScannerRun, repositoryProvenance.provenance)
            }
        }

        "readProvenances" should {
            "return all stored results" {
                val id = createIdentifier()

                val sourceArtifact = createRemoteArtifact()
                val artifactProvenance = createArtifactProvenance(sourceArtifact)
                packageProvenanceStorage.putProvenance(id, sourceArtifact, artifactProvenance)

                val vcsInfo = createVcsInfo()
                val repositoryProvenance = createRepositoryProvenance(vcsInfo)
                packageProvenanceStorage.putProvenance(id, vcsInfo, repositoryProvenance)

                packageProvenanceStorage.readProvenances(id) should containExactlyInAnyOrder(
                    artifactProvenance,
                    repositoryProvenance
                )
            }
        }
    }
}

private fun createIdentifier() = Identifier("Maven:org.apache.logging.log4j:log4j-api:2.14.1")

private fun createRemoteArtifact() =
    RemoteArtifact(
        url = "https://repo1.maven.org/maven2/org/apache/logging/" +
                "log4j/log4j-api/2.14.1/log4j-api-2.14.1-sources.jar",
        hash = Hash("b2327c47ca413c1ec183575b19598e281fcd74d8", HashAlgorithm.SHA1)
    )

private fun createVcsInfo() =
    VcsInfo(
        type = VcsType.GIT,
        url = "https://github.com/apache/logging-log4j2.git",
        revision = "be881e503e14b267fb8a8f94b6d15eddba7ed8c4",
        path = "testVcsPath"
    )

private fun createArtifactProvenance(artifactProvenance: RemoteArtifact) =
    ResolvedArtifactProvenance(
        provenance = ArtifactProvenance(
            RemoteArtifact(
                url = artifactProvenance.url,
                hash = Hash(
                    value = artifactProvenance.hash.value,
                    algorithm = artifactProvenance.hash.algorithm
                )
            )
        ),
    )

private fun createRepositoryProvenance(vcsInfo: VcsInfo) =
    ResolvedRepositoryProvenance(
        provenance = RepositoryProvenance(
            resolvedRevision = vcsInfo.revision,
            vcsInfo = VcsInfo(
                path = vcsInfo.path,
                url = vcsInfo.url,
                revision = vcsInfo.revision,
                type = VcsType(vcsInfo.type.toString())
            )
        ),
        clonedRevision = vcsInfo.revision,
        isFixedRevision = true
    )

private fun createErrorProvenance(errorMessage: String) =
    UnresolvedPackageProvenance(
        message = errorMessage
    )
