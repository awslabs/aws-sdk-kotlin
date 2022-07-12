package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.s3.transfermanager.data.Progress
import kotlinx.coroutines.Deferred

internal class DefaultOperation(private val deferredDelegate: Deferred<Unit>) : Operation, Deferred<Unit> by deferredDelegate {
    override val progress: Progress
        get() = TODO("Not yet implemented")
    override suspend fun pauseAndJoin(): PausedOperation {
        TODO("Not yet implemented")
    }
}
