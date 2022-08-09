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
public data class ProgressUpdater(var progress: Progress, val progressListener: ProgressListener) {
    private val mutex = Mutex()

    @InternalSdkApi
    public fun estimateProgressForUpload(file: File, chunkSize: Long) {
        if (file.isFile) {
            addTotalProgress(file.length(), chunkSize)
        } else if (file.isDirectory) {
            file.listFiles().forEach {
                estimateProgressForUpload(it, chunkSize)
            }
        }
    }

    @OptIn(InternalSdkApi::class)
    public suspend fun estimateProgressForDownload(singleObjectSize: Long, chunkSize: Long) {
        addTotalProgress(singleObjectSize, chunkSize)
    }

    @OptIn(InternalSdkApi::class)
    public suspend fun estimateProgressForDownload(objectFlow: Flow<Object>, chunkSize: Long): List<Object> {
        val objectList = mutableListOf<Object>()
        objectFlow.collect { obj ->
            addTotalProgress(obj.size, chunkSize)
            objectList.add(obj)
        }
        return objectList
    }

    /**
     * add single file/object workload to total progress
     */
    @InternalSdkApi
    private fun addTotalProgress(size: Long, chunkSize: Long) {
        val newProgress = progress.copy()
        newProgress.totalFilesToTransfer++
        newProgress.totalBytesToTransfer += size
        newProgress.totalChunksToTransfer += if ((size % chunkSize) == 0L) size / chunkSize else size / chunkSize + 1
        progress = newProgress
    }

    @InternalSdkApi
    public suspend fun addFilesTransferred(num: Long) {
        mutex.withLock {
            val newProgress = progress.copy()
            newProgress.filesTransferred += num
            if (newProgress.filesTransferred == newProgress.totalFilesToTransfer) {
                newProgress.isDone = true
            }
            progress = newProgress
        }
    }

    @InternalSdkApi
    public suspend fun addBytesChunksTransferred(byteLength: Long, chunkNum: Long) {
        mutex.withLock {
            val newProgress = progress.copy()
            newProgress.bytesTransferred += byteLength
            newProgress.chunksTransferred += chunkNum
            progress = newProgress
        }
    }
}
