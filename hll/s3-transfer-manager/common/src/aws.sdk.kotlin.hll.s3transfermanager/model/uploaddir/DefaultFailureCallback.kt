package aws.sdk.kotlin.hll.s3transfermanager.model.uploaddir

import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest

public enum class DefaultFailureCallback : FailureCallback {
    CANCEL {
        override fun onFailure(
            uploadDirectoryRequest: UploadDirectoryRequest,
            uploadFileRequest: UploadFileRequest,
            exception: Exception
        ) {
            TODO("Not yet implemented")
        }
    },
    IGNORE {
        override fun onFailure(
            uploadDirectoryRequest: UploadDirectoryRequest,
            uploadFileRequest: UploadFileRequest,
            exception: Exception
        ) {
            TODO("Not yet implemented")
        }
    },
}