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

package org.ossreviewtoolkit.server.model.repositories

import kotlinx.datetime.Instant

import org.ossreviewtoolkit.server.model.runs.EvaluatorRun
import org.ossreviewtoolkit.server.model.runs.OrtRuleViolation

/**
 * A repository of [evaluator runs][EvaluatorRun].
 */
interface EvaluatorRunRepository {
    /**
     * Create an evaluator run.
     */
    fun create(
        evaluatorJobId: Long,
        startTime: Instant,
        endTime: Instant,
        violations: List<OrtRuleViolation>
    ): EvaluatorRun

    /**
     * Get an evaluator run by [id]. Returns null if the evaluator run is not found.
     */
    fun get(id: Long): EvaluatorRun?

    /**
     * Get an evaluator run by [evaluatorJobId]. Returns null if the evaluator run is not found.
     */
    fun getByJobId(evaluatorJobId: Long): EvaluatorRun?
}