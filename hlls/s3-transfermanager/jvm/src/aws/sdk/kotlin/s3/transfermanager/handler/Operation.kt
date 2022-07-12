package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.s3.transfermanager.data.Progress
import kotlinx.coroutines.Deferred

public interface Operation : Deferred<Unit> {
// inherits await(), cancel(), cancelAndJoin(), etc. from Deferred from Job

    // class reflecting transfer progress and mark whether it is completed
    public val progress: Progress

    public suspend fun pauseAndJoin(): PausedOperation
}
