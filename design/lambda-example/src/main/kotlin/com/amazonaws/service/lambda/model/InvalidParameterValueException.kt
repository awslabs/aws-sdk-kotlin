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
package com.amazonaws.service.lambda.model

import software.aws.clientrt.ServiceException

// FIXME - soooo we don't have a great way to deal with this yet since we haven't gotten to the actual
// AWS specific protocols which actually tell you how to identify errors (e.g. through a header, payload __type/code, etc)
// Need to add a deserializer implementation + middleware + match configuration to the pipeline
class InvalidParameterValueException: ServiceException {
    constructor() : super()
    constructor(message: String?) : super(message)

    override val errorType: ErrorType = ErrorType.Client
    override val isRetryable: Boolean = false
    override val serviceName: String = "lambda"
    var type: String? = null
}
