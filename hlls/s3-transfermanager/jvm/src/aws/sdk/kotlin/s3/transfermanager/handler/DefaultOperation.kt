package aws.sdk.kotlin.s3.transfermanager.handler

import aws.sdk.kotlin.s3.transfermanager.data.Progress

public class DefaultOperation : Operation {
    override val progress: Progress
        get() = TODO("Not yet implemented")
    override suspend fun pauseAndJoin(): PausedOperation {
        TODO("Not yet implemented")
    }
}
