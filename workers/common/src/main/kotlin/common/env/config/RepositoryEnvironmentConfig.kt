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

package org.ossreviewtoolkit.server.workers.common.env.config

import kotlinx.serialization.Serializable

/**
 * Type definition to describe the generic section of the environment configuration that lists the environment
 * definitions. The section contains different types of definitions with different properties and is therefore
 * represented as a map of lists. The keys of the map declare the type of the definitions, such as *maven* or *npm*.
 * The list then contains the corresponding definitions with their specific properties. This structure is evaluated by
 * a factory that creates concrete class instances out of it.
 */
typealias RepositoryEnvironmentDefinitions = Map<String, List<Map<String, String>>>

/**
 * A data class defining the structure of the environment configuration file. The file is loaded by deserializing it
 * into an instance of this class.
 */
@Serializable
internal data class RepositoryEnvironmentConfig(
    /** The list of infrastructure services declared for this repository. */
    val infrastructureServices: List<RepositoryInfrastructureService>,

    /** A map with environment definitions of different types. */
    val environmentDefinitions: RepositoryEnvironmentDefinitions = emptyMap(),

    /** A flag that determines how semantic errors in the configuration file should be treated. */
    val strict: Boolean = true
)
