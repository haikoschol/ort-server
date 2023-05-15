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

package org.ossreviewtoolkit.server.storage

import java.io.InputStream

/**
 * A data class to represent an entry in a storage.
 *
 * Instances of this class are returned by the [StorageProvider.read] function. They allow access to the actual data
 * and can contain some additional metadata.
 *
 * The data is provided as an [InputStream]. It is in the responsibility of a storage client to close it after it has
 * been consumed. To simplify this, this class implements the [AutoCloseable] interface.
 */
data class StorageEntry(
    /**
     * A stream to obtain the data of this entry. The stream can be consumed once and should then be closed by the
     * caller.
     */
    val data: InputStream,

    /** The content type associated with this entry if any. */
    val contentType: String?
) : AutoCloseable {
    override fun close() {
        data.close()
    }
}