package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.s3.transfermanager.data.Progress
import aws.sdk.kotlin.s3.transfermanager.listener.ProgressListener
import aws.sdk.kotlin.services.s3.model.Object
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

@InternalSdkApi
public data class ProgressUpdater(var progress: Progress, val progressListener: ProgressListener, val chunkSize: Long) {
    private val mutex = Mutex()

    @InternalSdkApi
    public fun estimateProgressForUpload(file: File) {
        if (progress.totalFilesToTransfer == 0L) {
            discoverProgressForUpload(file)
            progressListener.onProgress(progress)
        }
    }

    private fun discoverProgressForUpload(file: File) {
        if (file.isFile) {
            addTotalProgress(file.length())
        } else if (file.isDirectory) {
            file.listFiles().forEach {
                discoverProgressForUpload(it)
            }
        }
    }

    @InternalSdkApi
    public fun estimateProgressForDownload(singleObjectSize: Long) {
        addTotalProgress(singleObjectSize)
        progressListener.onProgress(progress)
    }

    @InternalSdkApi
    public suspend fun estimateProgressForDownload(objectFlow: Flow<Object>): List<Object> {
        val objectList = mutableListOf<Object>()
        objectFlow.collect { obj ->
            addTotalProgress(obj.size)
            objectList.add(obj)
        }
        progressListener.onProgress(progress)
        return objectList
    }

    @InternalSdkApi
    public suspend fun updateProgress(fileNum: Long, byteLength: Long) {
        val newProgress = mutex.withLock {
            val filesTransferred = progress.filesTransferred + fileNum
            val isDone = (filesTransferred == progress.totalFilesToTransfer)
            val chunks = (byteLength + chunkSize - 1) / chunkSize
            progress.copy(
                filesTransferred = filesTransferred,
                bytesTransferred = progress.bytesTransferred + byteLength,
                chunksTransferred = progress.chunksTransferred + chunks,
                isDone = isDone
            ).also { progress = it }
        }
        progressListener.onProgress(newProgress)
    }

    /**
     * add single file/object workload to total progress
     */
    private fun addTotalProgress(size: Long) {
        val chunks = (size + chunkSize - 1) / chunkSize
        progress = progress.copy(
            totalFilesToTransfer = progress.totalFilesToTransfer + 1,
            totalBytesToTransfer = progress.totalBytesToTransfer + size,
            totalChunksToTransfer = progress.totalChunksToTransfer + chunks
        )
    }
}
