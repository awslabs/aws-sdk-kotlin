/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime

import software.aws.clientrt.ClientException
import software.aws.clientrt.ServiceErrorMetadata
import software.aws.clientrt.ServiceException
import software.aws.clientrt.util.AttributeKey

public open class AwsErrorMetadata : ServiceErrorMetadata() {
    public companion object {
        public val ErrorCode: AttributeKey<String> = AttributeKey("AwsErrorCode")
        public val ErrorMessage: AttributeKey<String> = AttributeKey("AwsErrorMessage")
        public val RequestId: AttributeKey<String> = AttributeKey("AwsRequestId")
    }

    /**
     * Returns the error code associated with the response
     */
    public val errorCode: String
        get() = attributes.getOrNull(ErrorCode) ?: ""

    /**
     * Returns the human readable error message. For errors with a `message` field as part of the model
     * this will match the `message` property of the exception.
     */
    public val errorMessage: String?
        get() = attributes.getOrNull(ErrorMessage)

    /**
     * The request ID that was returned by the called service
     */
    public val requestId: String
        get() = attributes.getOrNull(RequestId) ?: ""
}

/**
 * Base class for all AWS modeled service exceptions
 */
public open class AwsServiceException : ServiceException {

    public constructor() : super()

    public constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)

    public constructor(cause: Throwable?) : super(cause)

    override val sdkErrorMetadata: AwsErrorMetadata = AwsErrorMetadata()
}

/**
 * Base class for all exceptions originating from the AWS runtime
 */
public open class ClientException : ClientException {

    public constructor() : super()

    public constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)

    public constructor(cause: Throwable?) : super(cause)
}

/**
 * An exception that is thrown when the returned (error) response is not known by this version of the SDK
 * (i.e. the modeled error code is not known)
 */
public class UnknownServiceErrorException : AwsServiceException {

    public constructor() : super()

    public constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)

    public constructor(cause: Throwable?) : super(cause)
}
