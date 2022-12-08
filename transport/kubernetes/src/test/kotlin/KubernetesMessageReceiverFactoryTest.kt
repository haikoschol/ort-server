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

package org.ossreviewtoolkit.server.transport.kubernetes

import com.typesafe.config.ConfigFactory

import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.shouldBe

import org.ossreviewtoolkit.server.model.orchestrator.AnalyzeRequest
import org.ossreviewtoolkit.server.transport.AnalyzerEndpoint
import org.ossreviewtoolkit.server.transport.Message
import org.ossreviewtoolkit.server.transport.MessageHeader
import org.ossreviewtoolkit.utils.test.shouldNotBeNull

class KubernetesMessageReceiverFactoryTest : StringSpec({
    "Messages can be received via the Kubernetes transport" {
        val payload = AnalyzeRequest(1)
        val header = MessageHeader(token = "testToken", traceId = "testTraceId")

        val env = mapOf(
            "token" to header.token,
            "traceId" to header.traceId,
            "payload" to "{\"analyzerJobId\":${payload.analyzerJobId}}"
        )

        withEnvironment(env) {
            val keyPrefix = "analyzer.receiver"
            val configMap = mapOf(
                "$keyPrefix.type" to KubernetesConfig.TRANSPORT_NAME,
                "$keyPrefix.namespace" to "test-namespace",
                "$keyPrefix.imageName" to "busybox"
            )

            val config = ConfigFactory.parseMap(configMap)

            var receivedMessage: Message<AnalyzeRequest>? = null
            KubernetesMessageReceiverFactory().createReceiver(AnalyzerEndpoint, config) { message ->
                receivedMessage = message
            }

            receivedMessage.shouldNotBeNull {
                this.header.token shouldBe header.token
                this.header.traceId shouldBe header.traceId
                this.payload shouldBe payload
            }
        }
    }
})