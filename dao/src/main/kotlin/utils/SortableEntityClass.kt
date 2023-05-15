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

package org.ossreviewtoolkit.server.dao.utils

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.sql.SizedIterable

import org.ossreviewtoolkit.server.model.util.ListQueryParameters

/**
 * A specialized base class for entities with support for sorting. This class differs from its superclass
 * [LongEntityClass] by the fact that it is associated with a [SortableTable].
 */
open class SortableEntityClass<out E : LongEntity>(
    /** The [SortableTable] associated with this entity. */
    val sortableTable: SortableTable
) : LongEntityClass<E>(sortableTable) {
    /**
     * Query all entities from this class and apply the given [parameters].
     */
    fun list(parameters: ListQueryParameters): SizedIterable<E> =
        all().apply(sortableTable, parameters)
}