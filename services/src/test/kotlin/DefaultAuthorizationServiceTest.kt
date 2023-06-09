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

package org.ossreviewtoolkit.server.services

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.WordSpec

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import org.ossreviewtoolkit.server.clients.keycloak.Group
import org.ossreviewtoolkit.server.clients.keycloak.GroupId
import org.ossreviewtoolkit.server.clients.keycloak.GroupName
import org.ossreviewtoolkit.server.clients.keycloak.KeycloakClient
import org.ossreviewtoolkit.server.clients.keycloak.Role
import org.ossreviewtoolkit.server.clients.keycloak.RoleId
import org.ossreviewtoolkit.server.clients.keycloak.RoleName
import org.ossreviewtoolkit.server.dao.test.mockkTransaction
import org.ossreviewtoolkit.server.model.Organization
import org.ossreviewtoolkit.server.model.Product
import org.ossreviewtoolkit.server.model.Repository
import org.ossreviewtoolkit.server.model.RepositoryType
import org.ossreviewtoolkit.server.model.authorization.OrganizationPermission
import org.ossreviewtoolkit.server.model.authorization.OrganizationRole
import org.ossreviewtoolkit.server.model.authorization.ProductPermission
import org.ossreviewtoolkit.server.model.authorization.ProductRole
import org.ossreviewtoolkit.server.model.authorization.RepositoryPermission
import org.ossreviewtoolkit.server.model.authorization.RepositoryRole
import org.ossreviewtoolkit.server.model.repositories.OrganizationRepository
import org.ossreviewtoolkit.server.model.repositories.ProductRepository
import org.ossreviewtoolkit.server.model.repositories.RepositoryRepository

class DefaultAuthorizationServiceTest : WordSpec({
    val organizationId = 1L
    val productId = 2L
    val repositoryId = 3L

    "createOrganizationPermissions" should {
        "create the correct Keycloak roles" {
            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
            }

            val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

            service.createOrganizationPermissions(organizationId)

            coVerify(exactly = 1) {
                OrganizationPermission.getRolesForOrganization(organizationId).forEach {
                    keycloakClient.createRole(RoleName(it), ROLE_DESCRIPTION)
                }
            }
        }
    }

    "deleteOrganizationPermissions" should {
        "delete the correct Keycloak permissions" {
            val keycloakClient = mockk<KeycloakClient> {
                coEvery { deleteRole(any()) } returns mockk()
            }

            val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

            service.deleteOrganizationPermissions(organizationId)

            coVerify(exactly = 1) {
                OrganizationPermission.getRolesForOrganization(organizationId).forEach {
                    keycloakClient.deleteRole(RoleName(it))
                }
            }
        }
    }

    "createOrganizationRoles" should {
        val keycloakClient = mockk<KeycloakClient> {
            coEvery { createRole(any(), any()) } returns mockk()
            coEvery { getRole(any()) } answers {
                Role(id = RoleId(firstArg<String>()), name = RoleName(firstArg<String>()))
            }
            coEvery { addCompositeRole(any(), any()) } returns mockk()
            coEvery { createGroup(any()) } returns mockk()
            coEvery { getGroup(any<GroupName>()) } answers {
                Group(id = GroupId(firstArg<String>()), name = GroupName(firstArg<String>()), emptySet())
            }
            coEvery { addGroupClientRole(any(), any()) } returns mockk()
        }

        val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

        service.createOrganizationRoles(organizationId)

        "create the correct Keycloak roles" {
            coVerify(exactly = 1) {
                OrganizationRole.values().forEach { role ->
                    val roleName = RoleName(role.roleName(organizationId))
                    keycloakClient.createRole(roleName, ROLE_DESCRIPTION)
                }
            }
        }

        "add the correct permission roles as composites" {
            coVerify(exactly = 1) {
                OrganizationRole.values().forEach { role ->
                    val roleName = RoleName(role.roleName(organizationId))
                    role.permissions.forEach {
                        keycloakClient.addCompositeRole(roleName, RoleId(it.roleName(organizationId)))
                    }
                }
            }
        }

        "create a group for each role" {
            coVerify(exactly = 1) {
                OrganizationRole.values().forEach { role ->
                    val groupName = role.groupName(organizationId)
                    val roleName = role.roleName(organizationId)
                    keycloakClient.createGroup(GroupName(groupName))
                    keycloakClient.addGroupClientRole(
                        GroupId(groupName),
                        Role(RoleId(roleName), RoleName(roleName))
                    )
                }
            }
        }
    }

    "deleteOrganizationRoles" should {
        val keycloakClient = mockk<KeycloakClient> {
            coEvery { deleteRole(any()) } returns mockk()
            coEvery { getGroup(any<GroupName>()) } answers {
                Group(id = GroupId(firstArg<String>()), name = GroupName(firstArg<String>()), emptySet())
            }
            coEvery { deleteGroup(any()) } returns mockk()
        }

        val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

        service.deleteOrganizationRoles(organizationId)

        "delete the correct Keycloak roles" {
            coVerify(exactly = 1) {
                OrganizationRole.getRolesForOrganization(organizationId).forEach {
                    keycloakClient.deleteRole(RoleName(it))
                }
            }
        }

        "delete the Keycloak group" {
            coVerify(exactly = 1) {
                OrganizationRole.values().forEach { role ->
                    keycloakClient.deleteGroup(GroupId(role.groupName(organizationId)))
                }
            }
        }
    }

    "createProductPermissions" should {
        "create the correct Keycloak roles" {
            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
            }

            val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

            service.createProductPermissions(productId)

            coVerify(exactly = 1) {
                ProductPermission.getRolesForProduct(productId).forEach {
                    keycloakClient.createRole(RoleName(it), ROLE_DESCRIPTION)
                }
            }
        }
    }

    "deleteProductPermissions" should {
        "delete the correct Keycloak permissions" {
            val keycloakClient = mockk<KeycloakClient> {
                coEvery { deleteRole(any()) } returns mockk()
            }

            val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

            service.deleteProductPermissions(productId)

            coVerify(exactly = 1) {
                ProductPermission.getRolesForProduct(productId).forEach {
                    keycloakClient.deleteRole(RoleName(it))
                }
            }
        }
    }

    "createProductRoles" should {
        val keycloakClient = mockk<KeycloakClient> {
            coEvery { createRole(any(), any()) } returns mockk()
            coEvery { getRole(any()) } answers {
                Role(id = RoleId(firstArg<String>()), name = RoleName(firstArg<String>()))
            }
            coEvery { addCompositeRole(any(), any()) } returns mockk()
            coEvery { createGroup(any()) } returns mockk()
            coEvery { getGroup(any<GroupName>()) } answers {
                Group(id = GroupId(firstArg<String>()), name = GroupName(firstArg<String>()), emptySet())
            }
            coEvery { addGroupClientRole(any(), any()) } returns mockk()
        }

        val organizationRepository = mockk<OrganizationRepository> {
            every { this@mockk.get(any()) } returns Organization(id = organizationId, name = "organization")
        }

        val productRepository = mockk<ProductRepository> {
            every { this@mockk.get(any()) } returns
                    Product(id = productId, organizationId = organizationId, name = "product")
        }

        val service =
            DefaultAuthorizationService(keycloakClient, mockk(), organizationRepository, productRepository, mockk())

        service.createProductRoles(productId)

        "create the correct Keycloak roles" {
            coVerify(exactly = 1) {
                ProductRole.values().forEach { role ->
                    val roleName = RoleName(role.roleName(productId))
                    keycloakClient.createRole(roleName, ROLE_DESCRIPTION)
                }
            }
        }

        "add the correct permission roles as composites" {
            coVerify(exactly = 1) {
                ProductRole.values().forEach { role ->
                    val roleName = RoleName(role.roleName(productId))
                    role.permissions.forEach {
                        keycloakClient.addCompositeRole(roleName, RoleId(it.roleName(productId)))
                    }
                }
            }
        }

        "add the roles as composites to the parent roles" {
            coVerify(exactly = 1) {
                ProductRole.values().forEach { role ->
                    OrganizationRole.values().find { it.includedProductRole == role }?.let { orgRole ->
                        keycloakClient.addCompositeRole(
                            RoleName(orgRole.roleName(organizationId)),
                            RoleId(role.roleName(productId))
                        )
                    }
                }
            }
        }

        "create a group for each role" {
            coVerify(exactly = 1) {
                ProductRole.values().forEach { role ->
                    val groupName = role.groupName(productId)
                    val roleName = role.roleName(productId)
                    keycloakClient.createGroup(GroupName(groupName))
                    keycloakClient.addGroupClientRole(
                        GroupId(groupName),
                        Role(RoleId(roleName), RoleName(roleName))
                    )
                }
            }
        }
    }

    "deleteProductRoles" should {
        val keycloakClient = mockk<KeycloakClient> {
            coEvery { deleteRole(any()) } returns mockk()
            coEvery { getGroup(any<GroupName>()) } answers {
                Group(id = GroupId(firstArg<String>()), name = GroupName(firstArg<String>()), emptySet())
            }
            coEvery { deleteGroup(any()) } returns mockk()
        }

        val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

        service.deleteProductRoles(productId)

        "delete the correct Keycloak roles" {
            coVerify(exactly = 1) {
                ProductRole.getRolesForProduct(productId).forEach {
                    keycloakClient.deleteRole(RoleName(it))
                }
            }
        }

        "delete the Keycloak group" {
            coVerify(exactly = 1) {
                ProductRole.values().forEach { role ->
                    keycloakClient.deleteGroup(GroupId(role.groupName(productId)))
                }
            }
        }
    }

    "createRepositoryPermissions" should {
        "create the correct Keycloak roles" {
            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
            }

            val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

            service.createRepositoryPermissions(repositoryId)

            coVerify(exactly = 1) {
                RepositoryPermission.getRolesForRepository(repositoryId).forEach {
                    keycloakClient.createRole(RoleName(it), ROLE_DESCRIPTION)
                }
            }
        }
    }

    "deleteRepositoryPermissions" should {
        "delete the correct Keycloak permissions" {
            val keycloakClient = mockk<KeycloakClient> {
                coEvery { deleteRole(any()) } returns mockk()
            }

            val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

            service.deleteRepositoryPermissions(repositoryId)

            coVerify(exactly = 1) {
                RepositoryPermission.getRolesForRepository(repositoryId).forEach {
                    keycloakClient.deleteRole(RoleName(it))
                }
            }
        }
    }

    "createRepositoryRoles" should {
        val keycloakClient = mockk<KeycloakClient> {
            coEvery { createRole(any(), any()) } returns mockk()
            coEvery { getRole(any()) } answers {
                Role(id = RoleId(firstArg<String>()), name = RoleName(firstArg<String>()))
            }
            coEvery { addCompositeRole(any(), any()) } returns mockk()
            coEvery { createGroup(any()) } returns mockk()
            coEvery { getGroup(any<GroupName>()) } answers {
                Group(id = GroupId(firstArg<String>()), name = GroupName(firstArg<String>()), emptySet())
            }
            coEvery { addGroupClientRole(any(), any()) } returns mockk()
        }

        val productRepository = mockk<ProductRepository> {
            every { this@mockk.get(any()) } returns
                    Product(id = productId, organizationId = organizationId, name = "product")
        }

        val repositoryRepository = mockk<RepositoryRepository> {
            every { this@mockk.get(any()) } returns
                    Repository(
                        id = repositoryId,
                        organizationId = organizationId,
                        productId = productId,
                        type = RepositoryType.GIT,
                        url = "https://example.com/repo.git"
                    )
        }

        val service =
            DefaultAuthorizationService(keycloakClient, mockk(), mockk(), productRepository, repositoryRepository)

        service.createRepositoryRoles(repositoryId)

        "create the correct Keycloak roles" {
            coVerify(exactly = 1) {
                RepositoryRole.values().forEach { role ->
                    val roleName = RoleName(role.roleName(repositoryId))
                    keycloakClient.createRole(roleName, ROLE_DESCRIPTION)
                }
            }
        }

        "add the correct permission roles as composites" {
            coVerify(exactly = 1) {
                RepositoryRole.values().forEach { role ->
                    val roleName = RoleName(role.roleName(repositoryId))
                    role.permissions.forEach {
                        keycloakClient.addCompositeRole(roleName, RoleId(it.roleName(repositoryId)))
                    }
                }
            }
        }

        "add the roles as composites to the parent roles" {
            coVerify(exactly = 1) {
                RepositoryRole.values().forEach { role ->
                    ProductRole.values().find { it.includedRepositoryRole == role }?.let { productRole ->
                        keycloakClient.addCompositeRole(
                            RoleName(productRole.roleName(productId)),
                            RoleId(role.roleName(repositoryId))
                        )
                    }
                }
            }
        }

        "create a group for each role" {
            coVerify(exactly = 1) {
                RepositoryRole.values().forEach { role ->
                    val groupName = role.groupName(repositoryId)
                    val roleName = role.roleName(repositoryId)
                    keycloakClient.createGroup(GroupName(groupName))
                    keycloakClient.addGroupClientRole(
                        GroupId(groupName),
                        Role(RoleId(roleName), RoleName(roleName))
                    )
                }
            }
        }
    }

    "deleteRepositoryRoles" should {
        val keycloakClient = mockk<KeycloakClient> {
            coEvery { deleteRole(any()) } returns mockk()
            coEvery { getGroup(any<GroupName>()) } answers {
                Group(id = GroupId(firstArg<String>()), name = GroupName(firstArg<String>()), emptySet())
            }
            coEvery { deleteGroup(any()) } returns mockk()
        }

        val service = DefaultAuthorizationService(keycloakClient, mockk(), mockk(), mockk(), mockk())

        service.deleteRepositoryRoles(repositoryId)

        "delete the correct Keycloak roles" {
            coVerify(exactly = 1) {
                RepositoryRole.getRolesForRepository(repositoryId).forEach {
                    keycloakClient.deleteRole(RoleName(it))
                }
            }
        }

        "delete the Keycloak group" {
            coVerify(exactly = 1) {
                RepositoryRole.values().forEach { role ->
                    keycloakClient.deleteGroup(GroupId(role.groupName(repositoryId)))
                }
            }
        }
    }

    "synchronizePermissions" should {
        val org = Organization(id = 1L, name = "org")
        val prod = Product(id = 1L, organizationId = org.id, name = "prod")
        val repo = Repository(
            id = 1L,
            organizationId = org.id,
            productId = prod.id,
            type = RepositoryType.GIT,
            url = "https://example.org/repo.git"
        )

        val organizationRepository = mockk<OrganizationRepository> {
            every { list(any()) } returns listOf(org)
        }

        val productRepository = mockk<ProductRepository> {
            every { list(any()) } returns listOf(prod)
        }

        val repositoryRepository = mockk<RepositoryRepository> {
            every { list(any()) } returns listOf(repo)
        }

        fun createService(keycloakClient: KeycloakClient) =
            DefaultAuthorizationService(
                keycloakClient,
                mockk(),
                organizationRepository,
                productRepository,
                repositoryRepository
            )

        "create missing organization roles" {
            val existingRole = OrganizationPermission.READ.roleName(org.id)

            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
                coEvery { getRoles() } returns setOf(Role(id = RoleId("id"), RoleName(existingRole)))
            }

            val service = createService(keycloakClient)

            mockkTransaction { runBlocking { service.synchronizePermissions() } }

            coVerify(exactly = 0) {
                keycloakClient.createRole(RoleName(existingRole), any())
            }

            coVerify(exactly = 1) {
                (OrganizationPermission.getRolesForOrganization(org.id) - existingRole).forEach {
                    keycloakClient.createRole(RoleName(it), ROLE_DESCRIPTION)
                }
            }
        }

        "create missing product roles" {
            val existingRole = ProductPermission.READ.roleName(prod.id)

            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
                coEvery { getRoles() } returns setOf(Role(id = RoleId("id"), RoleName(existingRole)))
            }

            val service = createService(keycloakClient)

            mockkTransaction { runBlocking { service.synchronizePermissions() } }

            coVerify(exactly = 0) {
                keycloakClient.createRole(RoleName(existingRole), any())
            }

            coVerify(exactly = 1) {
                (ProductPermission.getRolesForProduct(prod.id) - existingRole).forEach {
                    keycloakClient.createRole(RoleName(it), ROLE_DESCRIPTION)
                }
            }
        }

        "create missing repository roles" {
            val existingRole = RepositoryPermission.READ.roleName(repo.id)

            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
                coEvery { getRoles() } returns setOf(Role(id = RoleId("id"), RoleName(existingRole)))
            }

            val service = createService(keycloakClient)

            mockkTransaction { runBlocking { service.synchronizePermissions() } }

            coVerify(exactly = 0) {
                keycloakClient.createRole(RoleName(existingRole), any())
            }

            coVerify(exactly = 1) {
                (RepositoryPermission.getRolesForRepository(repo.id) - existingRole).forEach {
                    keycloakClient.createRole(RoleName(it), ROLE_DESCRIPTION)
                }
            }
        }

        "remove unneeded organization roles" {
            val unneededRole = "${OrganizationPermission.rolePrefix(org.id)}_unneeded"

            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
                coEvery { deleteRole(any()) } returns mockk()
                coEvery { getRoles() } returns setOf(Role(id = RoleId("id"), RoleName(unneededRole)))
            }

            val service = createService(keycloakClient)

            mockkTransaction { runBlocking { service.synchronizePermissions() } }

            coVerify(exactly = 1) {
                keycloakClient.deleteRole(RoleName(unneededRole))
            }
        }

        "remove unneeded product roles" {
            val unneededRole = "${ProductPermission.rolePrefix(prod.id)}_unneeded"

            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
                coEvery { deleteRole(any()) } returns mockk()
                coEvery { getRoles() } returns setOf(Role(id = RoleId("id"), RoleName(unneededRole)))
            }

            val service = createService(keycloakClient)

            mockkTransaction { runBlocking { service.synchronizePermissions() } }

            coVerify(exactly = 1) {
                keycloakClient.deleteRole(RoleName(unneededRole))
            }
        }

        "remove unneeded repository roles" {
            val unneededRole = "${RepositoryPermission.rolePrefix(repo.id)}_unneeded"

            val keycloakClient = mockk<KeycloakClient> {
                coEvery { createRole(any(), any()) } returns mockk()
                coEvery { deleteRole(any()) } returns mockk()
                coEvery { getRoles() } returns setOf(Role(id = RoleId("id"), RoleName(unneededRole)))
            }

            val service = createService(keycloakClient)

            mockkTransaction { runBlocking { service.synchronizePermissions() } }

            coVerify(exactly = 1) {
                keycloakClient.deleteRole(RoleName(unneededRole))
            }
        }
    }
})
