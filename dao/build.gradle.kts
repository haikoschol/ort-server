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

plugins {
    `java-test-fixtures`

    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxSerialization)
}

group = "org.ossreviewtoolkit.server"

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(projects.config.configSpi)
    implementation(projects.model)
    implementation(projects.utils.config)

    api(libs.exposedDao)
    api(libs.koinCore)

    implementation(libs.bundles.flyway)
    implementation(libs.exposedCore)
    implementation(libs.exposedJson)
    implementation(libs.exposedKotlinDatetime)
    implementation(libs.hikari)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.postgres)
    implementation(libs.typesafeConfig)

    runtimeOnly(libs.exposedJdbc)
    runtimeOnly(libs.logback)

    testImplementation(testFixtures(projects.config.configSpi))

    testImplementation(libs.kotestAssertionsCore)
    testImplementation(libs.kotestAssertionsKtor)
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.mockk)

    testFixturesApi(projects.model)

    testFixturesImplementation(projects.config.configSpi)

    testFixturesImplementation(libs.flywayCore)
    testFixturesImplementation(libs.koinTest)
    testFixturesImplementation(libs.kotestExtensionsTestContainer)
    testFixturesImplementation(libs.kotestRunnerJunit5)
    testFixturesImplementation(libs.mockk)
    testFixturesImplementation(libs.testContainers)
    testFixturesImplementation(libs.testContainersPostgresql)
}
