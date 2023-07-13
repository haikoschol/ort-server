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

@file:Suppress("TooManyFunctions")

package org.ossreviewtoolkit.server.workers.common

import java.io.File
import java.net.URI

import kotlinx.datetime.toJavaInstant

import org.ossreviewtoolkit.model.AdvisorCapability as OrtAdvisorCapability
import org.ossreviewtoolkit.model.AdvisorDetails as OrtAdvisorDetails
import org.ossreviewtoolkit.model.AdvisorRecord as OrtAdvisorRecord
import org.ossreviewtoolkit.model.AdvisorResult as OrtAdvisorResult
import org.ossreviewtoolkit.model.AdvisorRun as OrtAdvisorRun
import org.ossreviewtoolkit.model.AdvisorSummary as OrtAdvisorSummary
import org.ossreviewtoolkit.model.AnalyzerResult as OrtAnalyzerResult
import org.ossreviewtoolkit.model.AnalyzerRun as OrtAnalyzerRun
import org.ossreviewtoolkit.model.ArtifactProvenance as OrtArtifactProvenance
import org.ossreviewtoolkit.model.CopyrightFinding as OrtCopyrightFinding
import org.ossreviewtoolkit.model.Defect as OrtDefect
import org.ossreviewtoolkit.model.DependencyGraph as OrtDependencyGraph
import org.ossreviewtoolkit.model.DependencyGraphEdge as OrtDependencyGraphEdge
import org.ossreviewtoolkit.model.DependencyGraphNode as OrtDependencyGraphNode
import org.ossreviewtoolkit.model.EvaluatorRun as OrtEvaluatorRun
import org.ossreviewtoolkit.model.Hash as OrtHash
import org.ossreviewtoolkit.model.HashAlgorithm.Companion as OrtHashAlgorithm
import org.ossreviewtoolkit.model.Identifier as OrtIdentifier
import org.ossreviewtoolkit.model.Issue
import org.ossreviewtoolkit.model.LicenseFinding as OrtLicenseFinding
import org.ossreviewtoolkit.model.LicenseSource
import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.model.Package as OrtPackage
import org.ossreviewtoolkit.model.PackageLinkage as OrtPackageLinkage
import org.ossreviewtoolkit.model.Project as OrtProject
import org.ossreviewtoolkit.model.ProvenanceResolutionResult as OrtProvenanceResolutionResult
import org.ossreviewtoolkit.model.RemoteArtifact as OrtRemoteArtifact
import org.ossreviewtoolkit.model.Repository as OrtRepository
import org.ossreviewtoolkit.model.RepositoryProvenance as OrtRepositoryProvenance
import org.ossreviewtoolkit.model.RootDependencyIndex as OrtRootDependencyIndex
import org.ossreviewtoolkit.model.RuleViolation as OrtRuleViolation
import org.ossreviewtoolkit.model.ScanResult as OrtScanResult
import org.ossreviewtoolkit.model.ScanSummary as OrtScanSummary
import org.ossreviewtoolkit.model.ScannerDetails as OrtScannerDetails
import org.ossreviewtoolkit.model.ScannerRun as OrtScannerRun
import org.ossreviewtoolkit.model.Severity as OrtSeverity
import org.ossreviewtoolkit.model.TextLocation as OrtTextLocation
import org.ossreviewtoolkit.model.UnknownProvenance as OrtUnknownProvenance
import org.ossreviewtoolkit.model.VcsInfo as OrtVcsInfo
import org.ossreviewtoolkit.model.VcsType as OrtVcsType
import org.ossreviewtoolkit.model.Vulnerability as OrtVulnerability
import org.ossreviewtoolkit.model.VulnerabilityReference as OrtVulnerabilityReference
import org.ossreviewtoolkit.model.config.AdvisorConfiguration as OrtAdvisorConfiguration
import org.ossreviewtoolkit.model.config.AnalyzerConfiguration as OrtAnalyzerConfiguration
import org.ossreviewtoolkit.model.config.ClearlyDefinedStorageConfiguration as OrtClearlyDefinedStorageConfiguration
import org.ossreviewtoolkit.model.config.FileArchiverConfiguration as OrtFileArchiveConfiguration
import org.ossreviewtoolkit.model.config.FileBasedStorageConfiguration as OrtFileBasedStorageConfiguration
import org.ossreviewtoolkit.model.config.FileStorageConfiguration as OrtFileStorageConfiguration
import org.ossreviewtoolkit.model.config.GitHubDefectsConfiguration as OrtGithubDefectsConfiguration
import org.ossreviewtoolkit.model.config.HttpFileStorageConfiguration as OrtHttpFileStorageConfiguration
import org.ossreviewtoolkit.model.config.LocalFileStorageConfiguration as OrtLocalFileStorageConfiguration
import org.ossreviewtoolkit.model.config.NexusIqConfiguration as OrtNexusIqConfiguration
import org.ossreviewtoolkit.model.config.OsvConfiguration as OrtOsvConfiguration
import org.ossreviewtoolkit.model.config.PackageManagerConfiguration as OrtPackageManagerConfiguration
import org.ossreviewtoolkit.model.config.PostgresConnection as OrtPostgresConnection
import org.ossreviewtoolkit.model.config.PostgresStorageConfiguration as OrtPostgresStorageConfiguration
import org.ossreviewtoolkit.model.config.ProvenanceStorageConfiguration as OrtProvenanceStorageConfiguration
import org.ossreviewtoolkit.model.config.RepositoryConfiguration as OrtRepositoryConfiguration
import org.ossreviewtoolkit.model.config.ScannerConfiguration as OrtScannerConfiguration
import org.ossreviewtoolkit.model.config.StorageType as OrtStorageTypd
import org.ossreviewtoolkit.model.config.Sw360StorageConfiguration as OrtSw360StorageConfiguration
import org.ossreviewtoolkit.model.config.VulnerableCodeConfiguration as OrtVulnerableCodeConfiguration
import org.ossreviewtoolkit.scanner.provenance.NestedProvenance as OrtNestedProvenance
import org.ossreviewtoolkit.scanner.provenance.NestedProvenanceScanResult as OrtNestedProvenanceScanResult
import org.ossreviewtoolkit.server.model.OrtRun
import org.ossreviewtoolkit.server.model.Repository
import org.ossreviewtoolkit.server.model.runs.AnalyzerConfiguration
import org.ossreviewtoolkit.server.model.runs.AnalyzerRun
import org.ossreviewtoolkit.server.model.runs.DependencyGraph
import org.ossreviewtoolkit.server.model.runs.DependencyGraphEdge
import org.ossreviewtoolkit.server.model.runs.DependencyGraphNode
import org.ossreviewtoolkit.server.model.runs.DependencyGraphRoot
import org.ossreviewtoolkit.server.model.runs.Environment
import org.ossreviewtoolkit.server.model.runs.EvaluatorRun
import org.ossreviewtoolkit.server.model.runs.Identifier
import org.ossreviewtoolkit.server.model.runs.OrtIssue as OrtServerIssue
import org.ossreviewtoolkit.server.model.runs.OrtRuleViolation as RuleViolation
import org.ossreviewtoolkit.server.model.runs.Package
import org.ossreviewtoolkit.server.model.runs.PackageManagerConfiguration
import org.ossreviewtoolkit.server.model.runs.Project
import org.ossreviewtoolkit.server.model.runs.RemoteArtifact
import org.ossreviewtoolkit.server.model.runs.VcsInfo
import org.ossreviewtoolkit.server.model.runs.advisor.AdvisorConfiguration
import org.ossreviewtoolkit.server.model.runs.advisor.AdvisorResult
import org.ossreviewtoolkit.server.model.runs.advisor.AdvisorRun
import org.ossreviewtoolkit.server.model.runs.advisor.Defect
import org.ossreviewtoolkit.server.model.runs.advisor.GithubDefectsConfiguration
import org.ossreviewtoolkit.server.model.runs.advisor.NexusIqConfiguration
import org.ossreviewtoolkit.server.model.runs.advisor.OsvConfiguration
import org.ossreviewtoolkit.server.model.runs.advisor.Vulnerability
import org.ossreviewtoolkit.server.model.runs.advisor.VulnerabilityReference
import org.ossreviewtoolkit.server.model.runs.advisor.VulnerableCodeConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.ArtifactProvenance
import org.ossreviewtoolkit.server.model.runs.scanner.ClearlyDefinedStorageConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.CopyrightFinding
import org.ossreviewtoolkit.server.model.runs.scanner.FileArchiveConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.FileBasedStorageConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.FileStorageConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.HttpFileStorageConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.KnownProvenance
import org.ossreviewtoolkit.server.model.runs.scanner.LicenseFinding
import org.ossreviewtoolkit.server.model.runs.scanner.LocalFileStorageConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.NestedProvenance
import org.ossreviewtoolkit.server.model.runs.scanner.NestedProvenanceScanResult
import org.ossreviewtoolkit.server.model.runs.scanner.PostgresConnection
import org.ossreviewtoolkit.server.model.runs.scanner.PostgresStorageConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.Provenance
import org.ossreviewtoolkit.server.model.runs.scanner.ProvenanceResolutionResult
import org.ossreviewtoolkit.server.model.runs.scanner.ProvenanceStorageConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.RepositoryProvenance
import org.ossreviewtoolkit.server.model.runs.scanner.ScanResult
import org.ossreviewtoolkit.server.model.runs.scanner.ScanSummary
import org.ossreviewtoolkit.server.model.runs.scanner.ScannerConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.ScannerDetail
import org.ossreviewtoolkit.server.model.runs.scanner.ScannerRun
import org.ossreviewtoolkit.server.model.runs.scanner.Sw360StorageConfiguration
import org.ossreviewtoolkit.server.model.runs.scanner.TextLocation
import org.ossreviewtoolkit.server.model.runs.scanner.UnknownProvenance
import org.ossreviewtoolkit.utils.common.enumSetOf
import org.ossreviewtoolkit.utils.ort.Environment as OrtEnvironment
import org.ossreviewtoolkit.utils.spdx.SpdxExpression
import org.ossreviewtoolkit.utils.spdx.SpdxSingleLicenseExpression

fun OrtRun.mapToOrt(
    repository: OrtRepository,
    analyzerRun: OrtAnalyzerRun? = null,
    advisorRun: OrtAdvisorRun? = null,
    scannerRun: OrtScannerRun? = null,
    evaluatorRun: OrtEvaluatorRun? = null
) = OrtResult(
    repository = repository,
    analyzer = analyzerRun,
    advisor = advisorRun,
    scanner = scannerRun,
    evaluator = evaluatorRun,
    labels = labels
)

fun Repository.mapToOrt(revision: String, path: String = "") =
    OrtRepository(
        vcs = OrtVcsInfo(
            type = OrtVcsType.forName(type.name),
            url = url,
            revision = revision,
            // TODO: The path of the repository is not stored at all.
            path = path
        ),
        // TODO: Nested repositories are not stored in the current implementation of the ORT server repository.
        nestedRepositories = emptyMap(),
        // TODO: The repository configuration is not stored at all.
        config = OrtRepositoryConfiguration()
    )

fun EvaluatorRun.mapToOrt() =
    OrtEvaluatorRun(
        startTime = startTime.toJavaInstant(),
        endTime = endTime.toJavaInstant(),
        violations = violations.map(RuleViolation::mapToOrt)
    )

fun RuleViolation.mapToOrt() =
    OrtRuleViolation(
        rule = rule,
        pkg = packageId?.mapToOrt(),
        license = license?.let { SpdxSingleLicenseExpression.parse(it) },
        licenseSource = licenseSource?.let { LicenseSource.valueOf(it) },
        severity = OrtSeverity.valueOf(severity),
        message = message,
        howToFix = howToFix
    )

fun AdvisorRun.mapToOrt() =
    OrtAdvisorRun(
        startTime = startTime.toJavaInstant(),
        endTime = endTime.toJavaInstant(),
        environment = environment.mapToOrt(),
        config = config.mapToOrt(),
        results = OrtAdvisorRecord(
            advisorRecords.entries.associateTo(sortedMapOf()) {
                it.key.mapToOrt() to it.value.map(AdvisorResult::mapToOrt)
            }
        )
    )

fun ScannerRun.mapToOrt() =
    OrtScannerRun(
        startTime = startTime.toJavaInstant(),
        endTime = endTime.toJavaInstant(),
        environment = environment.mapToOrt(),
        config = config.mapToOrt(),
        provenances = provenances.mapTo(mutableSetOf(), ProvenanceResolutionResult::mapToOrt),
        scanResults = scanResults.mapTo(mutableSetOf(), ScanResult::mapToOrt),
        files = emptySet()
    )

fun ProvenanceResolutionResult.mapToOrt() =
    OrtProvenanceResolutionResult(
        id = id.mapToOrt(),
        packageProvenance = packageProvenance?.mapToOrt(),
        subRepositories = subRepositories.mapValues { it.value.mapToOrt() },
        packageProvenanceResolutionIssue = packageProvenanceResolutionIssue?.mapToOrt(),
        nestedProvenanceResolutionIssue = nestedProvenanceResolutionIssue?.mapToOrt()
    )

fun NestedProvenanceScanResult.mapToOrt() =
    OrtNestedProvenanceScanResult(
        nestedProvenance = nestedProvenance.mapToOrt(),
        scanResults = scanResults.entries.associate { (provenance, results) ->
            provenance.mapToOrt() to results.map(ScanResult::mapToOrt)
        }
    )

fun ScanResult.mapToOrt() =
    OrtScanResult(
        provenance = provenance.mapToOrt(),
        scanner = scanner.mapToOrt(),
        summary = summary.mapToOrt(),
        additionalData = additionalData
    )

fun ScannerDetail.mapToOrt() = OrtScannerDetails(name, version, configuration)

fun ScanSummary.mapToOrt() =
    OrtScanSummary(
        startTime = startTime.toJavaInstant(),
        endTime = endTime.toJavaInstant(),
        licenseFindings = licenseFindings.mapTo(mutableSetOf(), LicenseFinding::mapToOrt),
        copyrightFindings = copyrightFindings.mapTo(mutableSetOf(), CopyrightFinding::mapToOrt),
        issues = issues.map(OrtServerIssue::mapToOrt)
    )

fun CopyrightFinding.mapToOrt() = OrtCopyrightFinding(statement, location.mapToOrt())

fun LicenseFinding.mapToOrt() = OrtLicenseFinding(
    license = SpdxExpression.Companion.parse(spdxLicense),
    location = location.mapToOrt(),
    score = score
)

fun TextLocation.mapToOrt() = OrtTextLocation(path, startLine, endLine)

fun Provenance.mapToOrt() =
    when (this) {
        is ArtifactProvenance -> this.mapToOrt()
        is RepositoryProvenance -> this.mapToOrt()
        UnknownProvenance -> OrtUnknownProvenance
    }

fun NestedProvenance.mapToOrt() =
    OrtNestedProvenance(
        root = root.mapToOrt(),
        subRepositories = subRepositories.mapValues { it.value.mapToOrt() }
    )

fun KnownProvenance.mapToOrt() =
    when (this) {
        is ArtifactProvenance -> this.mapToOrt()
        is RepositoryProvenance -> this.mapToOrt()
    }

fun ArtifactProvenance.mapToOrt() = OrtArtifactProvenance(sourceArtifact.mapToOrt())

fun RepositoryProvenance.mapToOrt() = OrtRepositoryProvenance(vcsInfo.mapToOrt(), resolvedRevision)

fun ScannerConfiguration.mapToOrt() =
    OrtScannerConfiguration(
        skipConcluded = skipConcluded,
        archive = archive?.mapToOrt(),
        createMissingArchives = createMissingArchives,
        detectedLicenseMapping = detectedLicenseMappings,
        options = options,
        storages = storages?.mapNotNull { (name, storage) ->
            storage?.let {
                name to when (it) {
                    is PostgresStorageConfiguration -> it.mapToOrt()
                    is ClearlyDefinedStorageConfiguration -> it.mapToOrt()
                    is FileBasedStorageConfiguration -> it.mapToOrt()
                    is Sw360StorageConfiguration -> it.mapToOrt()
                }
            }
        }?.toMap(),
        storageReaders = storageReaders,
        storageWriters = storageWriters,
        ignorePatterns = ignorePatterns,
        provenanceStorage = provenanceStorage?.mapToOrt()
    )

fun ClearlyDefinedStorageConfiguration.mapToOrt() = OrtClearlyDefinedStorageConfiguration(serverUrl)

fun FileBasedStorageConfiguration.mapToOrt() =
    OrtFileBasedStorageConfiguration(
        backend = backend.mapToOrt(),
        type = OrtStorageTypd.valueOf(type)
    )

fun Sw360StorageConfiguration.mapToOrt() =
    OrtSw360StorageConfiguration(
        restUrl = restUrl,
        authUrl = authUrl,
        username = username,
        clientId = clientId
    )

fun ProvenanceStorageConfiguration.mapToOrt() =
    OrtProvenanceStorageConfiguration(
        fileStorage = fileStorage?.mapToOrt(),
        postgresStorage = postgresStorageConfiguration?.mapToOrt()
    )

fun FileArchiveConfiguration.mapToOrt() =
    OrtFileArchiveConfiguration(
        enabled = enabled,
        fileStorage = fileStorage?.mapToOrt(),
        postgresStorage = postgresStorage?.mapToOrt()
    )

fun FileStorageConfiguration.mapToOrt() =
    OrtFileStorageConfiguration(
        httpFileStorage = httpFileStorage?.mapToOrt(),
        localFileStorage = localFileStorage?.mapToOrt()
    )

fun HttpFileStorageConfiguration.mapToOrt() =
    OrtHttpFileStorageConfiguration(
        url = url,
        query = query,
        headers = headers
    )

fun LocalFileStorageConfiguration.mapToOrt() =
    OrtLocalFileStorageConfiguration(
        directory = File(directory),
        compression = compression
    )

fun PostgresStorageConfiguration.mapToOrt() =
    OrtPostgresStorageConfiguration(
        connection = connection.mapToOrt(),
        type = OrtStorageTypd.valueOf(type)
    )

fun PostgresConnection.mapToOrt() =
    OrtPostgresConnection(
        url = url,
        schema = schema,
        username = username,
        sslmode = sslMode,
        sslcert = sslCert,
        sslkey = sslKey,
        sslrootcert = sslRootCert,
        parallelTransactions = parallelTransactions
    )

fun AdvisorResult.mapToOrt() =
    OrtAdvisorResult(
        advisor = OrtAdvisorDetails(advisorName, capabilities.mapTo(enumSetOf(), OrtAdvisorCapability::valueOf)),
        summary = OrtAdvisorSummary(
            startTime = startTime.toJavaInstant(),
            endTime = endTime.toJavaInstant(),
            issues = issues.map(OrtServerIssue::mapToOrt)
        ),
        defects = defects.map(Defect::mapToOrt),
        vulnerabilities = vulnerabilities.map(Vulnerability::mapToOrt)
    )

fun Defect.mapToOrt() =
    OrtDefect(
        id = externalId,
        url = URI(url),
        title = title,
        state = state,
        severity = severity,
        description = description,
        creationTime = creationTime?.toJavaInstant(),
        modificationTime = modificationTime?.toJavaInstant(),
        closingTime = closingTime?.toJavaInstant(),
        fixReleaseVersion = fixReleaseVersion,
        fixReleaseUrl = fixReleaseUrl,
        labels = labels
    )

fun Vulnerability.mapToOrt() =
    OrtVulnerability(
        id = externalId,
        summary = summary,
        description = description,
        references = references.map(VulnerabilityReference::mapToOrt)
    )

fun VulnerabilityReference.mapToOrt() = OrtVulnerabilityReference(URI(url), scoringSystem, severity)

fun AnalyzerRun.mapToOrt() =
    OrtAnalyzerRun(
        startTime = startTime.toJavaInstant(),
        endTime = endTime.toJavaInstant(),
        environment = environment.mapToOrt(),
        config = config.mapToOrt(),
        result = OrtAnalyzerResult(
            projects = projects.mapTo(mutableSetOf(), Project::mapToOrt),
            // TODO: Currently, curations are not stored at all, therefore the mapping just creates the OrtPackage.
            packages = packages.mapTo(mutableSetOf()) { it.mapToOrt() },
            issues = issues.entries.associate { it.key.mapToOrt() to it.value.map(OrtServerIssue::mapToOrt) },
            dependencyGraphs = dependencyGraphs.mapValues { it.value.mapToOrt() }
        )
    )

fun Environment.mapToOrt() =
    OrtEnvironment(
        os = os,
        ortVersion = ortVersion,
        javaVersion = javaVersion,
        processors = processors,
        maxMemory = maxMemory,
        variables = variables,
        toolVersions = toolVersions
    )

fun AnalyzerConfiguration.mapToOrt() =
    OrtAnalyzerConfiguration(
        allowDynamicVersions = allowDynamicVersions,
        enabledPackageManagers = enabledPackageManagers,
        disabledPackageManagers = disabledPackageManagers,
        packageManagers = packageManagers?.mapValues { it.value.mapToOrt() }
    )

fun AdvisorConfiguration.mapToOrt() =
    OrtAdvisorConfiguration(
        gitHubDefects = githubDefectsConfiguration?.mapToOrt(),
        nexusIq = nexusIqConfiguration?.mapToOrt(),
        osv = osvConfiguration?.mapToOrt(),
        vulnerableCode = vulnerableCodeConfiguration?.mapToOrt(),
        // TODO: Reimplement the options for the ORT server AdvisorConfiguration to store options from type
        //       Map<String, Map<String, String> and not Map<String, String>.
        options = emptyMap()
    )

fun GithubDefectsConfiguration.mapToOrt() =
    OrtGithubDefectsConfiguration(null, endpointUrl, labelFilter, maxNumberOfIssuesPerRepository, parallelRequests)

fun NexusIqConfiguration.mapToOrt() = OrtNexusIqConfiguration(serverUrl, browseUrl, null, null)

fun OsvConfiguration.mapToOrt() = OrtOsvConfiguration(serverUrl)

fun VulnerableCodeConfiguration.mapToOrt() = OrtVulnerableCodeConfiguration(serverUrl, null)

fun PackageManagerConfiguration.mapToOrt() = OrtPackageManagerConfiguration(mustRunAfter, options)

fun DependencyGraph.mapToOrt() =
    OrtDependencyGraph(
        packages = packages.map(Identifier::mapToOrt),
        scopes = scopes.mapValues { it.value.map(DependencyGraphRoot::mapToOrt) },
        nodes = nodes.map(DependencyGraphNode::mapToOrt),
        edges = edges.map(DependencyGraphEdge::mapToOrt)
    )

fun DependencyGraphRoot.mapToOrt() = OrtRootDependencyIndex(root, fragment)

fun DependencyGraphEdge.mapToOrt() = OrtDependencyGraphEdge(from, to)

fun DependencyGraphNode.mapToOrt() =
    OrtDependencyGraphNode(
        pkg = pkg,
        fragment = fragment,
        linkage = OrtPackageLinkage.valueOf(linkage),
        issues = issues.map(OrtServerIssue::mapToOrt)
    )

fun Project.mapToOrt() =
    OrtProject(
        id = identifier.mapToOrt(),
        cpe = cpe,
        definitionFilePath = definitionFilePath,
        authors = authors,
        declaredLicenses = declaredLicenses,
        vcs = vcs.mapToOrt(),
        vcsProcessed = vcsProcessed.mapToOrt(),
        homepageUrl = homepageUrl,
        scopeNames = scopeNames.toSortedSet()
    )

fun OrtServerIssue.mapToOrt() = Issue(timestamp.toJavaInstant(), source, message, OrtSeverity.valueOf(severity))

fun Package.mapToOrt() =
    OrtPackage(
        id = identifier.mapToOrt(),
        purl = purl,
        cpe = cpe,
        authors = authors,
        declaredLicenses = declaredLicenses,
        description = description,
        homepageUrl = homepageUrl,
        binaryArtifact = binaryArtifact.mapToOrt(),
        sourceArtifact = sourceArtifact.mapToOrt(),
        vcs = vcs.mapToOrt(),
        vcsProcessed = vcsProcessed.mapToOrt(),
        isMetadataOnly = isMetadataOnly,
        isModified = isModified
    )

fun Identifier.mapToOrt() = OrtIdentifier(type, namespace, name, version)

fun RemoteArtifact.mapToOrt() =
    OrtRemoteArtifact(
        url = url,
        hash = OrtHash(
            value = hashValue,
            algorithm = OrtHashAlgorithm.fromString(hashAlgorithm)
        )
    )

fun VcsInfo.mapToOrt() = OrtVcsInfo(OrtVcsType.forName(type.name), url, revision, path)
