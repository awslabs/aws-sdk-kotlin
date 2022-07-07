package aws.sdk.kotlin.s3.transfermanager

import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.s3.transfermanager.handler.DefaultOperation
import aws.sdk.kotlin.s3.transfermanager.handler.Operation
import aws.sdk.kotlin.s3.transfermanager.listener.ProgressListener
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.fromFile
import java.io.File
import java.nio.file.Paths

internal class DefaultS3TransferManager(override val config: S3TransferManager.Config) : S3TransferManager {

    private val s3: S3Client = config.s3

    // TODO enable context receiver after figuring out asynchronous work
    /**
     * upload a single file or directory locally to S3 bucket
     */
    override suspend fun upload(from: String, to: S3Uri, progressListener: ProgressListener?): Operation {
//        val job = async {
//
//        }

        val localFile = File(from)
        // throw IllegalArgumentException if from path is invalid
        require(localFile.exists()) { "From path is invalid" }

        return if (localFile.isFile()) {
            uploadFile(localFile, to)
        } else if (localFile.isDirectory()) {
            uploadDirectory(localFile, to, progressListener)
        } else {
            throw IllegalArgumentException("From path is invalid")
        }
    }

    private suspend fun uploadFile(localFile: File, to: S3Uri): Operation {
        // for single file upload, generate multiple parallel PUT requests according to s3Path and send to S3 Client object
        // wait the S3 client to reply upload response, then use operator to listen to progress and control pausing and resuming

        // determine upload with single request or split parts request according to file size

        val fileSize = localFile.length()
        return if (fileSize <= config.chunkSize) { // for file smaller than config chunk size
            uploadWholeFile(localFile, to)
        } else { // for large file over config chunk size
            uploadFileParts(localFile, to)
        }
    }

    private suspend fun uploadWholeFile(localFile: File, to: S3Uri): Operation {
        s3.putObject {
            bucket = to.bucket
            key = to.key
            body = ByteStream.fromFile(localFile)
        }
        val operation = DefaultOperation()
        return operation
    }

    private suspend fun uploadFileParts(localFile: File, to: S3Uri): Operation {
        val fileSize = localFile.length()

        val chunkRanges = (0 until fileSize step config.chunkSize).map {
            it until minOf(it + config.chunkSize, fileSize)
        }

        // initialize multipart upload
        val createMultipartUploadResponse = s3.createMultipartUpload {
            bucket = to.bucket
            key = to.key
        }

        val completedParts = mutableListOf<CompletedPart>()

        // call uploadPart() iteratively to continue uploading
        chunkRanges.forEachIndexed { index, chunkRange ->
            val uploadPartResponse = s3.uploadPart {
                body = localFile.asByteStream(chunkRange)
                bucket = to.bucket
                key = to.key
                uploadId = createMultipartUploadResponse.uploadId
                partNumber = (index + 1)
            }
            completedParts.add(
                CompletedPart {
                    eTag = uploadPartResponse.eTag
                    partNumber = (index + 1)
                }
            )
        }

        // complete multipart upload
        s3.completeMultipartUpload {
            bucket = to.bucket
            key = to.key
            uploadId = createMultipartUploadResponse.uploadId
            multipartUpload { parts = completedParts }
        }

        val operation = DefaultOperation()
        return operation
    }

    private suspend fun uploadDirectory(localFile: File, to: S3Uri, progressListener: ProgressListener?): Operation {
        // for directory, just use double pointer to start from fileDirectory/s3Path and recursively traverse directory/path
        // and call upload() to recursively finish the directory upload level by level like this
//            direc1     from: Users/direc1 		to:key
//               |_ a.txt    from: Users/direc1/a.txt	 to:key/a.txt
//               |_direc2    from: Users/direc1/direc2	to: key/direc2
//                   |_b.jpg	from: Users/direc1/direc2/b.jpg	to: key/direc2/b.jpg
//               |_direc3	from: Users/direc1/direc3	to: key/direc3

        val subFiles = localFile.listFiles()
        subFiles.forEach {
            val subFrom = localFile.toPath().resolve(it.name).toString()

            val subKey = Paths.get(to.key, it.name).toString()

            val subTo = S3Uri(to.bucket, subKey) // next level recursion's to

            // need to consider listener and receiver suboperation in the future!!!
            upload(subFrom, subTo, progressListener)
        }

        val operation = DefaultOperation()
        return operation
    }

    override suspend fun download(from: S3Uri, to: String, progressListener: ProgressListener?): Operation {
        TODO("Not yet implemented")
    }
    override suspend fun copy(from: List<S3Uri>, to: S3Uri, progressListener: ProgressListener?): Operation {
        TODO("Not yet implemented")
    }
}
