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

package org.ossreviewtoolkit.server.model.runs.repository

import org.ossreviewtoolkit.server.model.runs.RemoteArtifact

data class PackageCurationData(
    val comment: String? = null,
    val purl: String? = null,
    val cpe: String? = null,
    val authors: Set<String>? = null,
    val concludedLicense: String? = null,
    val description: String? = null,
    val homepageUrl: String? = null,
    val binaryArtifact: RemoteArtifact? = null,
    val sourceArtifact: RemoteArtifact? = null,
    val vcs: VcsInfoCurationData? = null,
    val isMetadataOnly: Boolean? = null,
    val isModified: Boolean? = null,
    val declaredLicenseMapping: Map<String, String> = emptyMap()
)