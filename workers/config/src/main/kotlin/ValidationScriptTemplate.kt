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

package org.ossreviewtoolkit.server.workers.config

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports

import kotlinx.datetime.Instant

import org.ossreviewtoolkit.server.model.JobConfigurations
import org.ossreviewtoolkit.server.workers.common.context.WorkerContext
import org.ossreviewtoolkit.utils.scripting.OrtScriptCompilationConfiguration

/**
 * A template defining the interface for scripts that can validate the parameters of an ORT run.
 * Such scripts are passed a [WorkerContext] providing access to the current ORT run with all its parameters and the
 * hierarchy of the current repository. The return value is a [ConfigValidationResult]
 */
@KotlinScript(
    displayName = "ORT Server parameters validation script",
    fileExtension = "kts",
    compilationConfiguration = ValidationScriptCompilationConfiguration::class
)
open class ValidationScriptTemplate(
    /** The context containing the run and the parameters to be validated. */
    val context: WorkerContext,

    /** The current time. */
    val time: Instant
) {
    var validationResult: ConfigValidationResult = ConfigValidationResultSuccess(JobConfigurations())
}

class ValidationScriptCompilationConfiguration : ScriptCompilationConfiguration(
    OrtScriptCompilationConfiguration(),
    body = {
        defaultImports(
            "org.ossreviewtoolkit.server.model.*",
            "org.ossreviewtoolkit.server.model.runs.*",
            "org.ossreviewtoolkit.server.workers.config.*"
        )
    }
)