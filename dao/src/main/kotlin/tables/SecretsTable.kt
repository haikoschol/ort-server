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

package org.ossreviewtoolkit.server.dao.tables

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

import org.ossreviewtoolkit.server.model.Secret

object SecretsTable : LongIdTable("secrets") {
    val path = text("path")
    val name = text("name").nullable()
    val description = text("description").nullable()
    val organizationId = reference("organization_id", OrganizationsTable.id).nullable()
    val productId = reference("product_id", ProductsTable.id).nullable()
    val repositoryId = reference("repository_id", RepositoriesTable.id).nullable()
}

class SecretDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SecretDao>(SecretsTable)

    var path by SecretsTable.path
    var name by SecretsTable.name
    var description by SecretsTable.description
    var organization by OrganizationDao optionalReferencedOn SecretsTable.organizationId
    var product by ProductDao optionalReferencedOn SecretsTable.productId
    var repository by RepositoryDao optionalReferencedOn SecretsTable.repositoryId

    fun mapToModel() = Secret(
        id = id.value,
        path = path,
        name = name,
        description = description,
        organization = organization?.mapToModel(),
        product = product?.mapToModel(),
        repository = repository?.mapToModel()
    )
}
