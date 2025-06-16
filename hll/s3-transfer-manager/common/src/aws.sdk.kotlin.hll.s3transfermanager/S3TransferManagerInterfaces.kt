
package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ChecksumAlgorithm
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.io.Closeable

// TODO: Experimental API ?
public interface S3TransferManager: Closeable {
    public val config: Config

    public companion object {
        public fun Config(config: Config.Builder.() -> Unit = { }): Config =
            Config.Builder().apply(config).build()
    }

    public interface Config {
        public companion object {
            public fun Builder(): Builder = S3TransferManagerConfigBuilderImplementation()
        }

        public val client: S3Client
        public val interceptors: List<HttpInterceptor>
        public val targetPartSizeBytes: Long
        public val multipartUploadThresholdBytes: Long
        public val checksumValidationEnabled: Boolean
        public val checksumAlgorithm: ChecksumAlgorithm
        public val multipartDownloadType: DownloadType

        public fun toBuilder(): Builder

        public interface Builder {
            public var client: S3Client
            public var interceptors: MutableList<HttpInterceptor>
            public var targetPartSizeBytes: Long
            public var multipartUploadThresholdBytes: Long
            public var checksumValidationEnabled: Boolean
            public var checksumAlgorithm: ChecksumAlgorithm
            public var multipartDownloadType: DownloadType
            public fun build(): Config
        }
    }

    // TODO: From environment

    public fun uploadFile(input: UploadFileRequest) : UploadFileResponse
    public fun downloadFile(input: DownloadFileRequest) : DownloadFileResponse
    public fun uploadDirectory(input: UploadDirectoryRequest) : UploadDirectoryResponse
    public fun downloadDirectory(input: DownloadDirectoryRequest) : DownloadDirectoryResponse
    public fun trackTransfer(input: TrackTransferRequest) : TrackTransferResponse

    override fun close() {
        config.client.close()
    }
}

public enum class DownloadType {
    PART,
    RANGE,
}

public interface UploadFileRequest
public interface UploadFileResponse

public interface DownloadFileRequest
public interface DownloadFileResponse

public interface UploadDirectoryRequest
public interface UploadDirectoryResponse

public interface DownloadDirectoryRequest
public interface DownloadDirectoryResponse

public interface TrackTransferRequest
public interface TrackTransferResponse
