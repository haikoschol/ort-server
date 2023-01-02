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

package org.ossreviewtoolkit.server.dao.tables.runs.advisor

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

/**
 * A table to represent a key-value pair, which belongs to an option for an [AdvisorConfigurationsTable].
 */
object AdvisorConfigurationsOptionsTable : LongIdTable("advisor_configuration_options") {
    val advisorConfigurationId = reference(
        "advisor_configuration_id",
        AdvisorConfigurationsTable.id,
        ReferenceOption.CASCADE
    )
    val key = text("key")
    val value = text("value")
}

class AdvisorConfigurationsOptionDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AdvisorConfigurationsOptionDao>(AdvisorConfigurationsOptionsTable)

    var advisorConfiguration by AdvisorConfigurationDao referencedOn
            AdvisorConfigurationsOptionsTable.advisorConfigurationId
    var key by AdvisorConfigurationsOptionsTable.key
    var value by AdvisorConfigurationsOptionsTable.value
}