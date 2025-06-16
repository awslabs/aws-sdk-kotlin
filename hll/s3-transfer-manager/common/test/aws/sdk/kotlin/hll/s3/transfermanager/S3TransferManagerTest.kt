package aws.sdk.kotlin.hll.s3.transfermanager

import aws.sdk.kotlin.hll.s3transfermanager.DownloadType
import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ChecksumAlgorithm

class S3TransferManagerTest {
    private val x = S3TransferManager {
        client = S3Client {}
        interceptors = mutableListOf()
        checksumAlgorithm = ChecksumAlgorithm.Crc32
        multipartDownloadType = DownloadType.PART
    }.uploadFile(
        input = TODO()
    )

    // TODO: Use different HTTP engines and config options
    // TODO: Look at tests defined in SEP
}