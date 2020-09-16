/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.aws.kotlinsdk

import software.aws.clientrt.ClientException
import software.aws.clientrt.ServiceException

/**
 * Base class for all modeled service exceptions
 */
open class AwsServiceException : ServiceException {

    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    /**
     * The request ID that was returned by the called service
     */
    open var requestId: String = ""

    /**
     * Returns the error code associated with the response
     */
    open var errorCode: String = ""
}

/**
 * Base class for all exceptions originating from the AWS runtime
 */
open class ClientException : ClientException {

    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

/**
 * An exception that is thrown when the returned (error) response is not known by this version of the SDK
 * (i.e. the modeled error code is not known)
 */
class UnknownServiceException : AwsServiceException {
    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
