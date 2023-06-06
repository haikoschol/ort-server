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

import org.ossreviewtoolkit.server.model.InfrastructureService
import org.ossreviewtoolkit.server.model.Secret
import org.ossreviewtoolkit.server.model.util.ListQueryParameters
import org.ossreviewtoolkit.server.model.util.OptionalValue

/**
 * Repository interface to manage [InfrastructureService] entities.
 */
@Suppress("TooManyFunctions")
interface InfrastructureServiceRepository {
    /**
     * Create a new [InfrastructureService] from the given properties that belongs to either an organization with the
     * given [organizationId] or a product with the given [productId].
     */
    fun create(
        name: String,
        url: String,
        description: String?,
        usernameSecret: Secret,
        passwordSecret: Secret,
        organizationId: Long?,
        productId: Long?
    ): InfrastructureService

    /**
     * Return an [InfrastructureService] with properties matching the ones of the given [service] that is associated
     * with the given [ORT Run][runId]. Based on the provided [service], an already existing entity is searched.
     * If there is no match, a new one is created. Note that existing services associated with an organization or a
     * product will not be matched. This is because such services can be changed by users at any time.
     */
    fun getOrCreateForRun(service: InfrastructureService, runId: Long): InfrastructureService

    /**
     * Return a list with the [InfrastructureService]s that belong to the given [organization][organizationId]
     * according to the given [parameters].
     */
    fun listForOrganization(
        organizationId: Long,
        parameters: ListQueryParameters = ListQueryParameters.DEFAULT
    ): List<InfrastructureService>

    /**
     * Return the [InfrastructureService] with the given [name] that is assigned to the given
     * [organization][organizationId] or *null* if no such service exists.
     */
    fun getByOrganizationAndName(organizationId: Long, name: String): InfrastructureService?

    /**
     * Update selected properties of the [InfrastructureService] with the given [name] that is assigned to the given
     * [organization][organizationId].
     */
    fun updateForOrganizationAndName(
        organizationId: Long,
        name: String,
        url: OptionalValue<String>,
        description: OptionalValue<String?>,
        usernameSecret: OptionalValue<Secret>,
        passwordSecret: OptionalValue<Secret>
    ): InfrastructureService

    /**
     * Delete the [InfrastructureService] with the given [name] that is assigned to the given
     * [organization][organizationId]. Throw an exception if the service cannot be found.
     */
    fun deleteForOrganizationAndName(organizationId: Long, name: String)

    /**
     * Return a list with the [InfrastructureService]s that belong to the given [product][productId]
     * according to the given [parameters].
     */
    fun listForProduct(
        productId: Long,
        parameters: ListQueryParameters = ListQueryParameters.DEFAULT
    ): List<InfrastructureService>

    /**
     * Return the [InfrastructureService] with the given [name] that is assigned to the given
     * [organization][productId] or *null* if no such service exists.
     */
    fun getByProductAndName(productId: Long, name: String): InfrastructureService?

    /**
     * Update selected properties of the [InfrastructureService] with the given [name] that is assigned to the given
     * [product][productId].
     */
    fun updateForProductAndName(
        productId: Long,
        name: String,
        url: OptionalValue<String>,
        description: OptionalValue<String?>,
        usernameSecret: OptionalValue<Secret>,
        passwordSecret: OptionalValue<Secret>
    ): InfrastructureService

    /**
     * Delete the [InfrastructureService] with the given [name] that is assigned to the given [product][productId].
     * Throw an exception if the service cannot be found.
     */
    fun deleteForProductAndName(productId: Long, name: String)

    /**
     * Return a list with the [InfrastructureService]s that are associated with the given [ORT Run][runId]
     * according to the given [parameters].
     */
    fun listForRun(
        runId: Long,
        parameters: ListQueryParameters = ListQueryParameters.DEFAULT
    ): List<InfrastructureService>
}