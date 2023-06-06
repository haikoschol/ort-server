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

package org.ossreviewtoolkit.server.dao.repositories

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import org.jetbrains.exposed.dao.exceptions.EntityNotFoundException

import org.ossreviewtoolkit.server.dao.test.DatabaseTestExtension
import org.ossreviewtoolkit.server.dao.test.Fixtures
import org.ossreviewtoolkit.server.model.InfrastructureService
import org.ossreviewtoolkit.server.model.Organization
import org.ossreviewtoolkit.server.model.Product
import org.ossreviewtoolkit.server.model.Secret
import org.ossreviewtoolkit.server.model.util.ListQueryParameters
import org.ossreviewtoolkit.server.model.util.OptionalValue
import org.ossreviewtoolkit.server.model.util.OrderDirection
import org.ossreviewtoolkit.server.model.util.OrderField
import org.ossreviewtoolkit.server.model.util.asPresent

class DaoInfrastructureServiceRepositoryTest : WordSpec() {
    private lateinit var infrastructureServicesRepository: DaoInfrastructureServiceRepository
    private lateinit var secretRepository: DaoSecretRepository
    private lateinit var fixtures: Fixtures
    private lateinit var usernameSecret: Secret
    private lateinit var passwordSecret: Secret

    init {
        extension(
            DatabaseTestExtension { db ->
                infrastructureServicesRepository = DaoInfrastructureServiceRepository(db)
                secretRepository = DaoSecretRepository(db)
                fixtures = Fixtures(db)

                usernameSecret = secretRepository.create("p1", "user", null, fixtures.organization.id, null, null)
                passwordSecret = secretRepository.create("p2", "pass", null, fixtures.organization.id, null, null)
            }
        )

        "create" should {
            "create an infrastructure service for an organization" {
                val expectedService = createInfrastructureService(organization = fixtures.organization)

                val service = infrastructureServicesRepository.create(expectedService)

                service shouldBe expectedService

                val organizationServices =
                    infrastructureServicesRepository.listForOrganization(fixtures.organization.id)
                organizationServices shouldContainOnly listOf(service)
            }

            "create an infrastructure service for a product" {
                val expectedService = createInfrastructureService(product = fixtures.product)

                val service = infrastructureServicesRepository.create(expectedService)

                service shouldBe expectedService

                val productServices = infrastructureServicesRepository.listForProduct(fixtures.product.id)
                productServices shouldContainOnly listOf(service)
            }
        }

        "listForOrganization" should {
            "return all services assigned to an organization" {
                val orgService1 = createInfrastructureService(organization = fixtures.organization)
                val orgService2 = createInfrastructureService(organization = fixtures.organization, name = "other")
                val prodService = createInfrastructureService(product = fixtures.product, name = "productService")

                listOf(orgService1, prodService, orgService2).forEach { infrastructureServicesRepository.create(it) }

                val services = infrastructureServicesRepository.listForOrganization(fixtures.organization.id)

                services shouldContainExactlyInAnyOrder listOf(orgService1, orgService2)
            }

            "apply list query parameters" {
                val expectedServices = (1..8).map { idx ->
                    createInfrastructureService(name = "$SERVICE_NAME$idx", organization = fixtures.organization)
                }

                expectedServices.shuffled().forEach { infrastructureServicesRepository.create(it) }

                val parameters =
                    ListQueryParameters(sortFields = listOf(OrderField("name", OrderDirection.ASCENDING)), limit = 4)
                val services =
                    infrastructureServicesRepository.listForOrganization(fixtures.organization.id, parameters)

                services shouldContainExactly expectedServices.take(4)
            }
        }

        "listForProduct" should {
            "return all services assigned to a product" {
                val prodService1 = createInfrastructureService(product = fixtures.product)
                val prodService2 = createInfrastructureService(product = fixtures.product, name = "other")
                val orgService =
                    createInfrastructureService(organization = fixtures.organization, name = "productService")

                listOf(prodService1, orgService, prodService2).forEach { infrastructureServicesRepository.create(it) }

                val services = infrastructureServicesRepository.listForProduct(fixtures.product.id)

                services shouldContainExactlyInAnyOrder listOf(prodService1, prodService2)
            }

            "apply list query parameters" {
                val expectedServices = (1..8).map { idx ->
                    createInfrastructureService(name = "$SERVICE_NAME$idx", product = fixtures.product)
                }

                expectedServices.shuffled().forEach { infrastructureServicesRepository.create(it) }

                val parameters =
                    ListQueryParameters(sortFields = listOf(OrderField("name", OrderDirection.ASCENDING)), limit = 4)
                val services = infrastructureServicesRepository.listForProduct(fixtures.product.id, parameters)

                services shouldContainExactly expectedServices.take(4)
            }
        }

        "getOrCreateForRun" should {
            "create a new entity in the database" {
                val expectedService = createInfrastructureService()

                val service = infrastructureServicesRepository.getOrCreateForRun(expectedService, fixtures.ortRun.id)

                service shouldBe expectedService

                val runServices = infrastructureServicesRepository.listForRun(fixtures.ortRun.id)
                runServices shouldContainOnly listOf(service)
            }

            "reuse an already existing entity" {
                val otherRun = fixtures.createOrtRun()
                val expectedService = createInfrastructureService()
                val serviceForOtherRun =
                    infrastructureServicesRepository.getOrCreateForRun(expectedService, otherRun.id)

                val serviceForRun =
                    infrastructureServicesRepository.getOrCreateForRun(expectedService, fixtures.ortRun.id)

                serviceForRun shouldBe expectedService
                serviceForRun shouldBe serviceForOtherRun

                val runServices = infrastructureServicesRepository.listForRun(fixtures.ortRun.id)
                runServices shouldContainOnly listOf(serviceForRun)
            }

            "not reuse a service assigned to an organization" {
                val orgService = createInfrastructureService(organization = fixtures.organization)
                infrastructureServicesRepository.create(orgService)

                val runService = createInfrastructureService()
                val dbRunService = infrastructureServicesRepository.getOrCreateForRun(runService, fixtures.ortRun.id)

                dbRunService shouldNotBe orgService
            }

            "not reuse a service assigned to a product" {
                val prodService = createInfrastructureService(product = fixtures.product)
                infrastructureServicesRepository.create(prodService)

                val runService = createInfrastructureService()
                val dbRunService = infrastructureServicesRepository.getOrCreateForRun(runService, fixtures.ortRun.id)

                dbRunService shouldNotBe prodService
            }
        }

        "listForRun" should {
            "return all services assigned to a run" {
                val runService1 = createInfrastructureService(name = "run1")
                val runService2 = createInfrastructureService(name = "run2")
                val orgService = createInfrastructureService(name = "org", organization = fixtures.organization)
                val prodService = createInfrastructureService(name = "prod", product = fixtures.product)

                infrastructureServicesRepository.create(orgService)
                infrastructureServicesRepository.create(prodService)
                infrastructureServicesRepository.getOrCreateForRun(runService1, fixtures.ortRun.id)
                infrastructureServicesRepository.getOrCreateForRun(runService2, fixtures.ortRun.id)

                val runServices = infrastructureServicesRepository.listForRun(fixtures.ortRun.id)

                runServices shouldContainExactlyInAnyOrder listOf(runService1, runService2)
            }

            "apply list parameters" {
                val expectedServices = (1..8).map { idx ->
                    createInfrastructureService(name = "$SERVICE_NAME$idx")
                }

                expectedServices.shuffled()
                    .forEach { infrastructureServicesRepository.getOrCreateForRun(it, fixtures.ortRun.id) }

                val parameters =
                    ListQueryParameters(sortFields = listOf(OrderField("name", OrderDirection.ASCENDING)), limit = 4)
                val services = infrastructureServicesRepository.listForRun(fixtures.ortRun.id, parameters)

                services shouldContainExactly expectedServices.take(4)
            }
        }

        "getForOrganizationAndName" should {
            "return an existing service" {
                val expectedService = createInfrastructureService(organization = fixtures.organization)
                infrastructureServicesRepository.create(expectedService)

                val service =
                    infrastructureServicesRepository.getByOrganizationAndName(fixtures.organization.id, SERVICE_NAME)

                service shouldBe expectedService
            }

            "return null for a non-existing service" {
                infrastructureServicesRepository.create(
                    createInfrastructureService(organization = fixtures.organization)
                )

                val service = infrastructureServicesRepository.getByOrganizationAndName(
                    fixtures.organization.id,
                    "onExisting"
                )

                service should beNull()
            }
        }

        "getForProductAndName" should {
            "return an existing service" {
                val expectedService = createInfrastructureService(product = fixtures.product)
                infrastructureServicesRepository.create(expectedService)

                val service = infrastructureServicesRepository.getByProductAndName(fixtures.product.id, SERVICE_NAME)

                service shouldBe expectedService
            }

            "return null for a non-existing service" {
                infrastructureServicesRepository.create(createInfrastructureService(product = fixtures.product))

                val service = infrastructureServicesRepository.getByProductAndName(fixtures.product.id, "onExisting")

                service should beNull()
            }
        }

        "updateForOrganizationAndName" should {
            "update the properties of a service" {
                val newUser = secretRepository.create("p3", "newUser", null, fixtures.organization.id, null, null)
                val newPassword = secretRepository.create("p4", "newPass", null, fixtures.organization.id, null, null)
                val service = createInfrastructureService(organization = fixtures.organization)
                val updatedService = createInfrastructureService(
                    url = "https://repo.example.org/newRepo",
                    description = null,
                    usernameSecret = newUser,
                    passwordSecret = newPassword,
                    organization = fixtures.organization
                )

                infrastructureServicesRepository.create(service)

                val result = infrastructureServicesRepository.updateForOrganizationAndName(
                    fixtures.organization.id,
                    SERVICE_NAME,
                    updatedService.url.asPresent(),
                    updatedService.description.asPresent(),
                    updatedService.usernameSecret.asPresent(),
                    updatedService.passwordSecret.asPresent()
                )

                result shouldBe updatedService

                val dbService =
                    infrastructureServicesRepository.getByOrganizationAndName(fixtures.organization.id, SERVICE_NAME)
                dbService shouldBe updatedService
            }

            "fail for a non-existing service" {
                shouldThrow<EntityNotFoundException> {
                    infrastructureServicesRepository.updateForOrganizationAndName(
                        42L,
                        SERVICE_NAME,
                        OptionalValue.Absent,
                        OptionalValue.Absent,
                        OptionalValue.Absent,
                        OptionalValue.Absent
                    )
                }
            }
        }

        "updateForProductAndName" should {
            "update the properties of a service" {
                val newUser = secretRepository.create("p3", "newUser", null, fixtures.organization.id, null, null)
                val newPassword = secretRepository.create("p4", "newPass", null, fixtures.organization.id, null, null)
                val service = createInfrastructureService(product = fixtures.product)
                val updatedService = createInfrastructureService(
                    url = "https://repo.example.org/newRepo",
                    description = null,
                    usernameSecret = newUser,
                    passwordSecret = newPassword,
                    product = fixtures.product
                )

                infrastructureServicesRepository.create(service)

                val result = infrastructureServicesRepository.updateForProductAndName(
                    fixtures.product.id,
                    SERVICE_NAME,
                    updatedService.url.asPresent(),
                    updatedService.description.asPresent(),
                    updatedService.usernameSecret.asPresent(),
                    updatedService.passwordSecret.asPresent()
                )

                result shouldBe updatedService

                val dbService = infrastructureServicesRepository.getByProductAndName(fixtures.product.id, SERVICE_NAME)
                dbService shouldBe updatedService
            }

            "fail for a non-existing service" {
                shouldThrow<EntityNotFoundException> {
                    infrastructureServicesRepository.updateForProductAndName(
                        42L,
                        SERVICE_NAME,
                        OptionalValue.Absent,
                        OptionalValue.Absent,
                        OptionalValue.Absent,
                        OptionalValue.Absent
                    )
                }
            }
        }

        "deleteForOrganizationAndName" should {
            "delete an existing entity" {
                val service1 = createInfrastructureService(organization = fixtures.organization)
                val service2 =
                    createInfrastructureService(name = "I_will_survive", organization = fixtures.organization)

                infrastructureServicesRepository.create(service1)
                infrastructureServicesRepository.create(service2)

                infrastructureServicesRepository.deleteForOrganizationAndName(fixtures.organization.id, SERVICE_NAME)

                val orgServices = infrastructureServicesRepository.listForOrganization(fixtures.organization.id)
                orgServices shouldContainOnly listOf(service2)
            }

            "fail for a non-existing service" {
                infrastructureServicesRepository.create(
                    createInfrastructureService(organization = fixtures.organization)
                )

                shouldThrow<EntityNotFoundException> {
                    infrastructureServicesRepository.deleteForOrganizationAndName(
                        fixtures.organization.id,
                        "nonExisting"
                    )
                }
            }
        }

        "deleteForProductAndName" should {
            "delete an existing entity" {
                val service1 = createInfrastructureService(product = fixtures.product)
                val service2 = createInfrastructureService(name = "I_will_survive", product = fixtures.product)

                infrastructureServicesRepository.create(service1)
                infrastructureServicesRepository.create(service2)

                infrastructureServicesRepository.deleteForProductAndName(fixtures.product.id, SERVICE_NAME)

                val prodServices = infrastructureServicesRepository.listForProduct(fixtures.product.id)
                prodServices shouldContainOnly listOf(service2)
            }

            "fail for a non-existing service" {
                infrastructureServicesRepository.create(createInfrastructureService(product = fixtures.product))

                shouldThrow<EntityNotFoundException> {
                    infrastructureServicesRepository.deleteForProductAndName(fixtures.product.id, "nonExisting")
                }
            }
        }
    }

    /**
     * Convenience function to create a test [InfrastructureService] with default properties.
     */
    private fun createInfrastructureService(
        name: String = SERVICE_NAME,
        url: String = SERVICE_URL,
        description: String? = SERVICE_DESCRIPTION,
        usernameSecret: Secret = this.usernameSecret,
        passwordSecret: Secret = this.passwordSecret,
        organization: Organization? = null,
        product: Product? = null
    ): InfrastructureService =
        InfrastructureService(name, url, description, usernameSecret, passwordSecret, organization, product)
}

private const val SERVICE_NAME = "MyRepositoryService"
private const val SERVICE_URL = "https://repo.example.org/artifacts"
private const val SERVICE_DESCRIPTION = "This infrastructure service..."

/**
 * Create an infrastructure service in the database based on the given [service].
 */
private fun DaoInfrastructureServiceRepository.create(service: InfrastructureService): InfrastructureService =
    create(
        service.name,
        service.url,
        service.description,
        service.usernameSecret,
        service.passwordSecret,
        service.organization?.id,
        service.product?.id
    )