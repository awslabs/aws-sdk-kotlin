package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.runtime.InternalSdkApi
import kotlinx.coroutines.Deferred

public interface Operation : Deferred<Unit> {
// inherits await(), cancel(), cancelAndJoin(), etc. from Deferred from Job

    // class containing progress to reflect transfer progress and if it is finished
    @OptIn(InternalSdkApi::class)
    val progressUpdater: ProgressUpdater?

    public suspend fun pauseAndJoin(): PausedOperation
}
