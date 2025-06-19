package aws.sdk.kotlin.hll.s3transfermanager.model.uploaddir

import aws.sdk.kotlin.hll.s3transfermanager.model.ParameterRequiredException
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.smithy.kotlin.runtime.SdkDsl
import java.io.File

public class UploadDirectoryRequest private constructor(builder: Builder) {
    public val bucket: String = builder.bucket ?: throw ParameterRequiredException("Missing bucket")
    public val source: String = builder.source ?: throw ParameterRequiredException("Missing source")
    public val followSymbolicLinks: Boolean = builder.followSymbolicLinks ?: false
    public val recursive: Boolean = builder.recursive ?: false
    public val s3Delimiter: String = builder.s3Delimiter ?: "/"
    public val filter: ((File) -> Boolean) = builder.filter ?: { true }
    public val failurePolicy: FailureCallback = builder.failurePolicy ?: DefaultFailureCallback.CANCEL
    public val putObjectRequestCallBack: ((UploadFileRequest) -> UploadFileRequest) = builder.putObjectRequestCallback ?: { it } // TODO: Should this be a put object request (S3) or an upload object request (S3 TM)
    public val s3Prefix: String? = builder.s3Prefix

    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): UploadDirectoryRequest = Builder().apply(block).build()
    }

    @SdkDsl
    public class Builder {
        public var bucket: String? = null
        public var source: String? = null
        public var followSymbolicLinks: Boolean? = null
        public var recursive: Boolean? = null
        public var s3Prefix: String? = null
        public var filter: ((File) -> Boolean)? = null // TODO: Use KN file
        public var s3Delimiter: String? = null
        public var putObjectRequestCallback: ((UploadFileRequest) -> UploadFileRequest)? = null
        public var failurePolicy: FailureCallback? = null

            @PublishedApi
        internal fun build(): UploadDirectoryRequest = UploadDirectoryRequest(this)
    }
}