package aws.sdk.kotlin.s3.transfermanager.data

data class Progress(
    val totalFilesToTransfer: Long,
    val totalBytesToTransfer: Long,
    val totalChunksToTransfer: Long,
    val filesTransferred: Long,
    val bytesTransferred: Long,
    val chunksTransferred: Long,
    val isDone: Boolean
) {}