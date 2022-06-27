package aws.sdk.kotlin.s3.transfermanager.handler

//interface Operation: kotlinx.coroutines.Deferred<Unit> {
interface Operation {
// inherits await(), cancel(), cancelAndJoin(), etc. from Deferred from Job

    // class reflecting transfer progress and mark whether it is completed
//    val progress: Progress

//    suspend fun pauseAndJoin(): PausedOperation {
// close connection between Transfer Manager and S3 client
        // pass transfer end point, last modified time of object and file
        // and other transfer configuration to PausedOperation
//    }
    // Sets a flag that causes early/non-exceptional termination and calls join()

}
