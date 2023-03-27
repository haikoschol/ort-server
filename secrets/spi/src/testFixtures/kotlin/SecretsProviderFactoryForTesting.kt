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

package org.ossreviewtoolkit.server.secrets

import com.typesafe.config.Config

/**
 * A simple implementation of the [SecretsProviderFactory] interface for testing purposes that uses an in-memory
 * storage to manage its secrets. A newly created provider instance is already populated with a number of secrets.
 * This set can be modified using the CRUD operations.
 */
class SecretsProviderFactoryForTesting : SecretsProviderFactory {
    companion object {
        /** The name of this test provider implementation. */
        const val NAME = "secretsProviderForTesting"

        /**
         * Name of a configuration that defines a path that should cause the [SecretsProvider] to throw an exception.
         * This is used to test exception handling. If the property is undefined, the provider does not throw
         * exceptions.
         */
        const val ERROR_PATH_PROPERTY = "errorPath"

        /** A prefix under which the predefined secrets are stored. */
        private const val PREFIX = "secrets"

        /** Path to the predefined _password_ secret. */
        val PASSWORD_PATH = Path("$PREFIX.password")

        /** Path to the predefined _token_ secret. */
        val TOKEN_PATH = Path("$PREFIX.token")

        /** Path to the predefined _service_ secret. */
        val SERVICE_PATH = Path("$PREFIX.service")

        /** The predefined password secret. */
        val PASSWORD_SECRET = Secret("aS3ce3TPwd")

        /** The predefined token secret. */
        val TOKEN_SECRET = Secret("1234567890abcdefghijklmnopqrstuvwxyz")

        /** The predefined service secret. */
        val SERVICE_SECRET = Secret("db_data")

        /** The path under which the defined secrets are stored. */
        val PARENT_PATH = Path(PREFIX)

        /**
         * Return a map to be used as internal secret store that is already populated with the test secrets.
         */
        private fun createStorage(): MutableMap<Path, Secret> =
            mutableMapOf(
                PASSWORD_PATH to PASSWORD_SECRET,
                TOKEN_PATH to TOKEN_SECRET,
                SERVICE_PATH to SERVICE_SECRET
            )
    }

    override val name: String
        get() = NAME

    override fun createProvider(config: Config): SecretsProvider {
        val storage = createStorage()
        val errorPath = if (config.hasPath(ERROR_PATH_PROPERTY)) config.getString(ERROR_PATH_PROPERTY) else "."

        fun checkPath(path: Path): Path =
            path.takeUnless { it.path == errorPath } ?: throw IllegalArgumentException("Test exception")

        return object : SecretsProvider {
            override fun readSecret(path: Path): Secret? = storage[checkPath(path)]

            override fun writeSecret(path: Path, secret: Secret) {
                storage[checkPath(path)] = secret
            }

            override fun removeSecret(path: Path) {
                storage -= checkPath(path)
            }

            override fun listPaths(path: Path): Set<Path> {
                val prefix = "${checkPath(path).path}."

                return storage.keys.filter { it.path.startsWith(prefix) }.toSet()
            }
        }
    }
}