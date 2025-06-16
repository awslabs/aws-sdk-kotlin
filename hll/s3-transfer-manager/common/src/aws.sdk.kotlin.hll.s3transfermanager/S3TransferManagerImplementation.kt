package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ChecksumAlgorithm
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor

public fun S3TransferManager(
    config: S3TransferManager.Config.Builder.() -> Unit = { },
): S3TransferManager = S3TransferManagerImplementation(S3TransferManager.Config(config))

internal data class S3TransferManagerImplementation(
    override val config: S3TransferManager.Config,
) : S3TransferManager {
    override fun uploadFile(input: UploadFileRequest): UploadFileResponse {
        // TODO: How would this change the config on the fly ?
        TODO("Not yet implemented")
    }

    override fun downloadFile(input: DownloadFileRequest): DownloadFileResponse {
        TODO("Not yet implemented")
    }

    override fun uploadDirectory(input: UploadDirectoryRequest): UploadDirectoryResponse {
        TODO("Not yet implemented")
    }

    override fun downloadDirectory(input: DownloadDirectoryRequest): DownloadDirectoryResponse {
        TODO("Not yet implemented")
    }

    override fun trackTransfer(input: TrackTransferRequest): TrackTransferResponse {
        TODO("Not yet implemented")
    }
}

internal data class S3TransferManagerConfigImplementation(
    override val client: S3Client,
    override val interceptors: List<HttpInterceptor>,
    override val targetPartSizeBytes: Long,
    override val multipartUploadThresholdBytes: Long,
    override val checksumValidationEnabled: Boolean,
    override val checksumAlgorithm: ChecksumAlgorithm,
    override val multipartDownloadType: DownloadType,
) : S3TransferManager.Config {
    override fun toBuilder(): S3TransferManager.Config.Builder = S3TransferManager.Config.Builder()
}

internal class S3TransferManagerConfigBuilderImplementation : S3TransferManager.Config.Builder {
    override var client: S3Client = S3Client { httpClient = CrtHttpEngine() }
    override var interceptors: MutableList<HttpInterceptor> = mutableListOf()
    override var targetPartSizeBytes: Long = 8_000_000L
    override var multipartUploadThresholdBytes: Long = 16_000_000L
    override var checksumValidationEnabled: Boolean = true
    override var checksumAlgorithm: ChecksumAlgorithm = ChecksumAlgorithm.Crc32
    override var multipartDownloadType: DownloadType = DownloadType.PART

    override fun build() = S3TransferManagerConfigImplementation(
        client = this.client,
        interceptors = this.interceptors,
        targetPartSizeBytes = this.targetPartSizeBytes,
        multipartUploadThresholdBytes = this.multipartUploadThresholdBytes,
        checksumValidationEnabled = this.checksumValidationEnabled,
        checksumAlgorithm = this.checksumAlgorithm,
        multipartDownloadType = this.multipartDownloadType,
    )
}





