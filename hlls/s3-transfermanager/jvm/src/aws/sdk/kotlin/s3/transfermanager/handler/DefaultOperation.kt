package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.s3.transfermanager.data.Progress
import kotlinx.coroutines.Deferred

@InternalSdkApi
public class DefaultOperation(private val deferredDelegate: Deferred<Unit>) : Operation, Deferred<Unit> by deferredDelegate {
    override var progress: Progress = Progress()
//    get() = progress

    override suspend fun pauseAndJoin(): PausedOperation {
        TODO("Not yet implemented")
    }
}

@OptIn(InternalSdkApi::class)
public fun DefaultOperation(deferredDelegate: Deferred<Unit>, progress: Progress): DefaultOperation {
    val operation = DefaultOperation(deferredDelegate)
    operation.progress = progress
    return operation
}
