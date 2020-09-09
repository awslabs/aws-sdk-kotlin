/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.aws.kotlinsdk.http

import software.aws.clientrt.http.HttpStatusCode
import software.aws.clientrt.http.feature.HttpDeserialize

/**
 * Metadata container about a modeled service exception
 *
 * @property errorCode The unique error code name that services use to identify errors
 * @property deserializer The deserializer responsible for providing a [Throwable] instance of the actual exception
 * @property httpStatusCode The HTTP status code the error is returned with
 */
data class ExceptionMetadata(val errorCode: String, val deserializer: HttpDeserialize, val httpStatusCode: HttpStatusCode? = null)

/**
 * Container for modeled exceptions
 */
class ExceptionRegistry {
    // ErrorCode -> Meta
    private val errorsByCodeName = mutableMapOf<String, ExceptionMetadata>()

    /**
     * Register a modeled exception's metadata
     */
    fun register(metadata: ExceptionMetadata) {
        errorsByCodeName[metadata.errorCode] = metadata
    }

    /**
     * Get the exception metadata associated with the given [code] name
     */
    operator fun get(code: String?): ExceptionMetadata? = errorsByCodeName[code]
}
