/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime

import software.aws.clientrt.ClientException
import software.aws.clientrt.ServiceException

/**
 * Base class for all modeled service exceptions
 */
public open class AwsServiceException : ServiceException {

    public constructor() : super()

    public constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)

    /**
     * The request ID that was returned by the called service
     */
    public open var requestId: String = ""

    /**
     * Returns the error code associated with the response
     */
    public open var errorCode: String = ""
}

/**
 * Base class for all exceptions originating from the AWS runtime
 */
public open class ClientException : ClientException {

    public constructor() : super()

    public constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)
}

/**
 * An exception that is thrown when the returned (error) response is not known by this version of the SDK
 * (i.e. the modeled error code is not known)
 */
public class UnknownServiceErrorException : AwsServiceException {

    public constructor() : super()

    public constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)
}
