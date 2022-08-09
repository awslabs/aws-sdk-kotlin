package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.runtime.InternalSdkApi
import kotlinx.coroutines.Deferred

@InternalSdkApi
public class DefaultOperation(private val deferredDelegate: Deferred<Unit>, override val progressUpdater: ProgressUpdater? = null) : Operation, Deferred<Unit> by deferredDelegate {
    override suspend fun pauseAndJoin(): PausedOperation {
        TODO("Not yet implemented")
    }
}
