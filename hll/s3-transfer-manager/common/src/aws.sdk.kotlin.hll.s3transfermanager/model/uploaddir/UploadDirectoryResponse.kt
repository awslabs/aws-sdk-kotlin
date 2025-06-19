package aws.sdk.kotlin.hll.s3transfermanager.model.uploaddir

import aws.sdk.kotlin.hll.s3transfermanager.model.ParameterRequiredException
import aws.smithy.kotlin.runtime.SdkDsl

public class UploadDirectoryResponse private constructor(builder: Builder) {
    public val objectsUploaded: Long = builder.objectsUploaded ?: throw ParameterRequiredException("Missing objectsUploaded")
    public val objectsFailed: Long = builder.objectsFailed ?: throw ParameterRequiredException("Missing objectsFailed")

    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): UploadDirectoryResponse = Builder().apply(block).build()
    }

    @SdkDsl
    public class Builder {
        public var objectsUploaded: Long? = null
        public var objectsFailed: Long? = null

        @PublishedApi
        internal fun build(): UploadDirectoryResponse = UploadDirectoryResponse(this)
    }
}