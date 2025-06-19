package aws.sdk.kotlin.hll.s3transfermanager.model.uploaddir

import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest

public interface FailureCallback {
    public fun onFailure(
        uploadDirectoryRequest: UploadDirectoryRequest,
        uploadFileRequest: UploadFileRequest, // TODO: Upload file request or put object request ?
        exception: Exception,
    )
}