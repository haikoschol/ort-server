/*
 * Copyright (C) 2023 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
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

package org.eclipse.apoapsis.ortserver.transport.rabbitmq

import com.rabbitmq.client.ConnectionFactory

import io.kotest.core.spec.style.StringSpec

import org.eclipse.apoapsis.ortserver.model.orchestrator.AnalyzerWorkerError
import org.eclipse.apoapsis.ortserver.model.orchestrator.AnalyzerWorkerResult
import org.eclipse.apoapsis.ortserver.model.orchestrator.OrchestratorMessage
import org.eclipse.apoapsis.ortserver.transport.MessageHeader
import org.eclipse.apoapsis.ortserver.transport.json.JsonSerializer
import org.eclipse.apoapsis.ortserver.transport.rabbitmq.RabbitMqMessageConverter.toAmqpProperties
import org.eclipse.apoapsis.ortserver.transport.testing.TEST_QUEUE_NAME
import org.eclipse.apoapsis.ortserver.transport.testing.checkMessage
import org.eclipse.apoapsis.ortserver.transport.testing.startReceiver

class RabbitMqMessageReceiverFactoryTest : StringSpec() {
    init {
        "Messages can be received via the RabbitMQ transport" {
            val serializer = JsonSerializer.forType<OrchestratorMessage>()
            val config = startRabbitMqContainer("orchestrator", "receiver")

            val connectionFactory = ConnectionFactory().apply {
                setUri(config.getString("orchestrator.receiver.serverUri"))
            }
            connectionFactory.newConnection().use { connection ->
                val channel = connection.createChannel().also {
                    it.queueDeclare(
                        /* queue = */ TEST_QUEUE_NAME,
                        /* durable = */ false,
                        /* exclusive = */ false,
                        /* autoDelete = */ false,
                        /* arguments = */ emptyMap()
                    )
                }

                val messageQueue = startReceiver(config)

                val token1 = "token1"
                val traceId1 = "trace1"
                val runId1 = 1L
                val payload1 = AnalyzerWorkerError(1)
                val token2 = "token2"
                val traceId2 = "trace2"
                val runId2 = 2L
                val payload2 = AnalyzerWorkerResult(42)

                channel.basicPublish(
                    "",
                    TEST_QUEUE_NAME,
                    MessageHeader(token1, traceId1, runId1).toAmqpProperties(),
                    serializer.toJson(payload1).toByteArray()
                )

                channel.basicPublish(
                    "",
                    TEST_QUEUE_NAME,
                    MessageHeader(token2, traceId2, runId2).toAmqpProperties(),
                    serializer.toJson(payload2).toByteArray()
                )

                messageQueue.checkMessage(token1, traceId1, runId1, payload1)
                messageQueue.checkMessage(token2, traceId2, runId2, payload2)
            }
        }

        "Exceptions during message receiving are handled" {
            val serializer = JsonSerializer.forType<OrchestratorMessage>()
            val config = startRabbitMqContainer("orchestrator", "receiver")

            val connectionFactory = ConnectionFactory().apply {
                setUri(config.getString("orchestrator.receiver.serverUri"))
            }
            connectionFactory.newConnection().use { connection ->
                val channel = connection.createChannel().also {
                    it.queueDeclare(
                        /* queue = */ TEST_QUEUE_NAME,
                        /* durable = */ false,
                        /* exclusive = */ false,
                        /* autoDelete = */ false,
                        /* arguments = */ emptyMap()
                    )
                }

                val messageQueue = startReceiver(config)

                channel.basicPublish(
                    "",
                    TEST_QUEUE_NAME,
                    MessageHeader("tokenInvalid", "traceIdInvalid", -1).toAmqpProperties(),
                    "Invalid payload".toByteArray()
                )

                val token = "validtoken"
                val traceId = "validtrace"
                val runId = 10L
                val payload = AnalyzerWorkerResult(42)
                channel.basicPublish(
                    "",
                    TEST_QUEUE_NAME,
                    MessageHeader(token, traceId, runId).toAmqpProperties(),
                    serializer.toJson(payload).toByteArray()
                )

                messageQueue.checkMessage(token, traceId, runId, payload)
            }
        }
    }
}
