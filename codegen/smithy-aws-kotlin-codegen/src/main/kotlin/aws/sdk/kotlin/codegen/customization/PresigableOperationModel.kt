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
 * NOTE: This type intentionally uses serializable types to make future model migration easier.
 */
data class PresignableOperation(
    /**
     * ID of service presigning applies to
     */
    val serviceId: String,
    /**
     * Operation capable of presigning
     */
    val operationId: String,
    /**
     * (Optional) parameter in which presigned URL should be passed on behalf of the customer
     */
    val presignedParameterId: String?,
    /**
     * HEADER or QUERY_STRING depending on the service and operation.
     */
    val signingLocation: String,
    /**
     * List of header keys that should be included when generating the request signature
     */
    val signedHeaders: Set<String>,
    /**
     * (Optional) override of operation’s HTTP method
     */
    val methodOverride: String?,
    /**
     * true if operation will pass an unsigned body with the request
     */
    val hasBody: Boolean,
    /**
     * If true, map request parameters onto the query string of the presigned URL
     */
    val transformRequestToQueryString: Boolean
)

// This is the dejour model that may be replaced by the API model once presign state is available
internal val servicesWithOperationPresigners = listOf(
    PresignableOperation(
        "com.amazonaws.polly#Parrot_v1",
        "com.amazonaws.polly#SynthesizeSpeech",
        null,
        "QUERY_STRING",
        setOf("host"),
        "GET",
        hasBody = false,
        transformRequestToQueryString = true
    ),
    PresignableOperation(
        "com.amazonaws.s3#AmazonS3",
        "com.amazonaws.s3#GetObject",
        null,
        "HEADER",
        setOf("host", "x-amz-content-sha256", "X-Amz-Date", "Authorization"),
        null,
        hasBody = false,
        transformRequestToQueryString = false
    ),
    PresignableOperation(
        "com.amazonaws.s3#AmazonS3",
        "com.amazonaws.s3#PutObject",
        null,
        "HEADER",
        setOf("host", "x-amz-content-sha256", "X-Amz-Date", "Authorization"),
        null,
        hasBody = true,
        transformRequestToQueryString = false
    )/*,
    PresignableOperation(
        "com.amazonaws.sts#AWSSecurityTokenServiceV20110615",
        "com.amazonaws.sts#GetCallerIdentity",
        presignedParameterId = null,
        signingLocation = "HEADER",
        setOf("host", "X-Amz-Date", "content-type"),
        methodOverride = null,
        hasBody = true,
        transformRequestToQueryString = false
    )*/
)