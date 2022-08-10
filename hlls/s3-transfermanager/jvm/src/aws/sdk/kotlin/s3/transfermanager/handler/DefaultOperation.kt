package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.runtime.InternalSdkApi
import kotlinx.coroutines.Deferred

@InternalSdkApi
public class DefaultOperation(private val deferredDelegate: Deferred<Unit>, private val progressUpdater: ProgressUpdater?) : Operation, Deferred<Unit> by deferredDelegate {
    override val progress
        get() = progressUpdater?.progress

    override suspend fun pauseAndJoin(): PausedOperation {
        TODO("Not yet implemented")
    }
}
