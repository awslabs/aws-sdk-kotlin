package aws.sdk.kotlin.codegen.customization

/*
 * At the time of writing, presigned operations are unmodeled, however it may happen that traits are added
 * to API models in the future to encapsulate this functionality.  As such, this integration is written such
 * that codegen is driven from a du jour model of presign operations.  If and when this state is available
 * in the API model, this informal model should be removed and access to the Smithy model should be used
 * instead to drive codegen.
 */

/**
 * Represents a presignable operation.
 *
 * @property serviceId ID of service presigning applies to
 * @property operationId Operation capable of presigning
 * @property presignedParameterId (Optional) parameter in which presigned URL should be passed in the request
 * @property hasBody true if operation will pass an unsigned body with the request
 *
 */
data class PresignableOperation(
    val serviceId: String,
    val operationId: String,
    val presignedParameterId: String?,
    val hasBody: Boolean,
)

// This is the dejour model that may be replaced by the API model once presign state is available
internal val servicesWithOperationPresigners = setOf(
    PresignableOperation(
        "com.amazonaws.s3#AmazonS3",
        "com.amazonaws.s3#GetObject",
        null,
        hasBody = false,
    ),
    PresignableOperation(
        "com.amazonaws.s3#AmazonS3",
        "com.amazonaws.s3#PutObject",
        null,
        hasBody = true,
    ),
    PresignableOperation(
        "com.amazonaws.s3#AmazonS3",
        "com.amazonaws.s3#UploadPart",
        null,
        hasBody = true,
    ),
    // FIXME ~ Following operation signature fails service side.
    /*PresignableOperation(
        "com.amazonaws.sts#AWSSecurityTokenServiceV20110615",
        "com.amazonaws.sts#GetCallerIdentity",
        presignedParameterId = null,
        hasBody = true,
    )*/
)
