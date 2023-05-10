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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

import java.util.Locale

import org.jetbrains.exposed.sql.transactions.transaction

import org.ossreviewtoolkit.server.dao.QueryParametersException
import org.ossreviewtoolkit.server.dao.repositories.DaoOrganizationRepository
import org.ossreviewtoolkit.server.dao.repositories.DaoOrtRunRepository
import org.ossreviewtoolkit.server.dao.repositories.DaoProductRepository
import org.ossreviewtoolkit.server.dao.repositories.DaoRepositoryRepository
import org.ossreviewtoolkit.server.dao.tables.OrganizationDao
import org.ossreviewtoolkit.server.dao.test.DatabaseTestExtension
import org.ossreviewtoolkit.server.model.JobConfigurations
import org.ossreviewtoolkit.server.model.Organization
import org.ossreviewtoolkit.server.model.RepositoryType
import org.ossreviewtoolkit.server.model.util.ListQueryParameters
import org.ossreviewtoolkit.server.model.util.OrderDirection
import org.ossreviewtoolkit.server.model.util.OrderField

private const val COUNT = 32
private const val ORGANIZATION_NAME = "TestOrganization"
private const val ORGANIZATION_DESC = "Description"

/**
 * A test class that tests the generic mechanism of applying query parameters for list queries. The test uses the
 * repositories for organizations and repositories as example.
 */
class ListQueryTest : StringSpec() {
    private val organizationRepository = DaoOrganizationRepository()

    init {
        extension(DatabaseTestExtension())

        beforeTest {
            insertTestOrganizations()
        }

        "Entities can be ordered ascending" {
            val parameters = ListQueryParameters(sortFields = listOf(OrderField("name", OrderDirection.ASCENDING)))

            val organizations = query(parameters)

            checkOrganizations(organizations, 1..COUNT)
        }

        "Order fields can be case insensitive" {
            val parameters = ListQueryParameters(sortFields = listOf(OrderField("name", OrderDirection.ASCENDING)))

            val organizations = query(parameters)

            checkOrganizations(organizations, 1..COUNT)
        }

        "Entities can be ordered descending" {
            val parameters = ListQueryParameters(sortFields = listOf(OrderField("name", OrderDirection.DESCENDING)))

            val organizations = query(parameters)

            checkOrganizations(organizations, COUNT downTo 1)
        }

        "Entities can be ordered by multiple fields" {
            val repositoryUrl = "https://repo.example.org/test"
            val productRepository = DaoProductRepository()
            val repositoryRepository = DaoRepositoryRepository()

            val organization = organizationRepository.create("Another Organization", "for testing")
            val product = productRepository.create("TestProduct", null, organization.id)

            val repo1 = repositoryRepository.create(RepositoryType.GIT, repositoryUrl.appendIndex(1), product.id)
            val repo2 =
                repositoryRepository.create(RepositoryType.SUBVERSION, repositoryUrl.appendIndex(2), product.id)
            val repo3 = repositoryRepository.create(RepositoryType.GIT, repositoryUrl.appendIndex(3), product.id)

            val parameters = ListQueryParameters(
                sortFields = listOf(
                    OrderField("type", OrderDirection.DESCENDING),
                    OrderField("url", OrderDirection.ASCENDING)
                )
            )

            val repositories = transaction {
                repositoryRepository.listForProduct(product.id, parameters)
            }

            repositories shouldContainExactly listOf(repo2, repo1, repo3)
        }

        "Sorting is only allowed for properties marked as sortable" {
            val parameters = ListQueryParameters(
                sortFields = listOf(
                    OrderField("name", OrderDirection.ASCENDING),
                    OrderField("description", OrderDirection.ASCENDING)
                )
            )

            val exception = shouldThrow<QueryParametersException> {
                query(parameters)
            }

            exception.localizedMessage shouldContain "description"
        }

        "Logic property names are used to define the sort order" {
            val productRepository = DaoProductRepository()
            val repositoryRepository = DaoRepositoryRepository()
            val runRepository = DaoOrtRunRepository()

            val organization = organizationRepository.create("Run Organization", null)
            val product = productRepository.create("Run Product", null, organization.id)
            val repo =
                repositoryRepository.create(RepositoryType.GIT, "https://repo.example.org/run.git", product.id)
            val runs = (1..3).map {
                runRepository.create(repo.id, "test", JobConfigurations())
            }

            val parameters = ListQueryParameters(
                sortFields = listOf(
                    OrderField("createdAt", OrderDirection.ASCENDING)
                )
            )

            val runsFromQuery = transaction {
                runRepository.listForRepository(repo.id, parameters)
            }

            runsFromQuery shouldContainExactly runs
        }

        "A limit can be set for queries" {
            val parameters = ListQueryParameters(
                sortFields = listOf(OrderField("name", OrderDirection.ASCENDING)),
                limit = 8
            )

            val organizations = query(parameters)

            checkOrganizations(organizations, 1..8)
        }

        "A limit and an offset can be set for queries" {
            val parameters = ListQueryParameters(
                sortFields = listOf(OrderField("name", OrderDirection.DESCENDING)),
                limit = 8,
                offset = 17
            )

            val organizations = query(parameters)

            checkOrganizations(organizations, 15 downTo 8)
        }
    }

    /**
     * Insert a number of synthetic test organizations in random order.
     */
    private fun insertTestOrganizations() {
        (1..COUNT).toList().shuffled().forEach { index ->
            organizationRepository.create(ORGANIZATION_NAME.appendIndex(index), ORGANIZATION_DESC.appendIndex(index))
        }
    }

    /**
     * Return the results of a query for organizations that applies the given [parameters].
     */
    private fun query(parameters: ListQueryParameters): List<Organization> = transaction {
        OrganizationDao.list(parameters).map { it.mapToModel() }
    }

    /**
     * Check whether the given [list] contains the test entities in the expected [range].
     */
    private fun checkOrganizations(list: List<Organization>, range: IntProgression) {
        list shouldHaveSize range.count()

        val iterator = list.iterator()
        range.forEach { index ->
            val org = iterator.next()
            org.name shouldBe ORGANIZATION_NAME.appendIndex(index)
            org.description shouldBe ORGANIZATION_DESC.appendIndex(index)
        }
    }
}

/**
 * Return a string consisting of this string with the given [index] appended, making sure that 2 digits are used for
 * the index, so that the lexical order is correctly kept.
 */
private fun String.appendIndex(index: Int): String = String.format(Locale.ROOT, "%s%02d", this, index)
