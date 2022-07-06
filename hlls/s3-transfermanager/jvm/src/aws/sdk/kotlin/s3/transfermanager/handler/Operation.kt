package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.s3.transfermanager.data.Progress

// interface Operation: kotlinx.coroutines.Deferred<Unit> {
interface Operation {
// inherits await(), cancel(), cancelAndJoin(), etc. from Deferred from Job

    // class reflecting transfer progress and mark whether it is completed
    val progress: Progress

    suspend fun pauseAndJoin(): PausedOperation
}
