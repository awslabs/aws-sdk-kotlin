package aws.sdk.kotlin.s3.transfermanager

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.s3.transfermanager.handler.DefaultOperation
import aws.sdk.kotlin.s3.transfermanager.handler.Operation
import aws.sdk.kotlin.s3.transfermanager.listener.ProgressListener
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.completeMultipartUpload
import aws.sdk.kotlin.services.s3.copyObject
import aws.sdk.kotlin.services.s3.createMultipartUpload
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.headObject
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.HeadBucketResponse
import aws.sdk.kotlin.services.s3.model.HeadObjectResponse
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.sdk.kotlin.services.s3.putObject
import aws.sdk.kotlin.services.s3.uploadPart
import aws.sdk.kotlin.services.s3.uploadPartCopy
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.fromFile
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.copyTo
import aws.smithy.kotlin.runtime.io.writeChannel
import aws.smithy.kotlin.runtime.util.InternalApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.transform
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.createFile

internal class DefaultS3TransferManager(override val config: S3TransferManager.Config) : S3TransferManager {

    private val s3: S3Client = config.s3

    /**
     * upload a single file or directory locally to S3 bucket
     * starting from a main scope, generate children coroutine scope in such logic:
     *                        main coroutine
                                    | input file
                            parent coroutine
                    if file /	      		 \ if directory
            child coroutine		          give each subFile a separate coroutine
            uploadFIle/uploadFileParts    as their parent coroutine
                                                | recursively judging…
     */
    context(CoroutineScope)
    override fun upload(from: String, to: S3Uri, progressListener: ProgressListener?): Operation {
        val deferred = async<Unit> {
            val localFile = File(from)
            // throw IllegalArgumentException if from path is invalid
            require(localFile.exists()) { "From path is invalid" }
            if (s3.headBucketOrNull(to.bucket) == null) {
                throw java.lang.IllegalArgumentException("The bucket does not exist or has no access to it")
            }

            when {
                localFile.isFile() -> uploadFile(localFile, to)
                localFile.isDirectory() -> uploadDirectory(localFile, to, progressListener)
                else -> throw IllegalArgumentException("From path is invalid")
            }
        }

        return DefaultOperation(deferred)
    }

    context(CoroutineScope)
    private fun uploadFile(localFile: File, to: S3Uri) {
        // for single file upload, generate multiple parallel PUT requests according to s3Path and send to S3 Client object
        // wait the S3 client to reply upload response, then use operator to listen to progress and control pausing and resuming

        // determine upload with single request or split parts request according to file size
        val fileSize = localFile.length()
        if (fileSize <= config.chunkSize) { // for file smaller than config chunk size
            println("Downloading whole small file...")
            uploadWholeFile(localFile, to)
        } else { // for large file over config chunk size
            uploadFileParts(localFile, to)
        }
    }

    context(CoroutineScope)
    private fun uploadWholeFile(localFile: File, to: S3Uri) {
        async<Unit> {
            println("Start downloading from S3Client!!!")
            s3.putObject {
                bucket = to.bucket
                key = to.key
                body = ByteStream.fromFile(localFile)
            }
        }
    }

    context(CoroutineScope)
    @OptIn(InternalSdkApi::class)
    private fun uploadFileParts(localFile: File, to: S3Uri) {
        async<Unit> {
            val chunkRanges = partition(localFile.length(), config.chunkSize)

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
        }
    }

    context(CoroutineScope)
    private fun uploadDirectory(localFile: File, to: S3Uri, progressListener: ProgressListener?) {
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
    }

    /**
     * download a S3 bucket key object or key-prefix directory to local file system
     * for directory download, ideal result is like this
        S3:
    keyPrefix: bucket/path/to/a/key/
    valid objects:
    bucket/path/to/a/key/file1
    bucket/path/to/a/key/dir1/file2
    bucket/path/to/a/key/dir1/file3
    bucket/path/to/a/key/bar/file4
    bucket/path/to/a/key/bar/dir1/file5

    Files system:
    /foo/bar/key/
    downloaded files:
    /foo/bar/key/file1
    /foo/bar/key/dir1/file2
    /foo/bar/key/dir1/file3
    /foo/bar/key/bar/file4
    /foo/bar/key/bar/dir1/file5
     */
    context(CoroutineScope)
    @OptIn(InternalSdkApi::class)
    override fun download(from: S3Uri, to: String, progressListener: ProgressListener?): Operation {
        val deferred = async {
            if (s3.headBucketOrNull(from.bucket) == null) { // first check if bucket exists
                throw IllegalArgumentException("The bucket does not exist or has no access to it")
            }

            if (!from.key.endsWith('/')) {
                val response = s3.headObjectOrNull(from)
                if (response != null) {
                    val subTo = Paths.get(to).resolve(from.key.substringAfterLast('/')).toString()
                    downloadFile(response.contentLength, from, subTo)
                    return@async
                }
            }

            // check if the current key is a keyPrefix
            val keyPrefix = from.key.ensureEndsWith('/')

            val response = s3.listObjectsV2Paginated {
                bucket = from.bucket
                prefix = keyPrefix
            }

            if (response.firstOrNull()?.contents?.isNotEmpty() != true) {
                throw IllegalArgumentException("From S3 uri contains invalid key/keyPrefix")
            }

            response // Flow<ListObjectsV2Response>, a collection of pages
                .transform { it.contents?.forEach { obj -> emit(obj) } }
                .collect { obj ->
                    val key = obj.key!!
                    val s3Uri = S3Uri(from.bucket, key)
                    val keySuffix = key.substringAfter(keyPrefix)
                    val subTo = Paths.get(to, keySuffix).toString()
                    downloadFile(obj.size, s3Uri, subTo)
                }
        }

        return DefaultOperation(deferred)
    }

    /**
     * download a single object to local path
     * from is object's bucket-key
     * to refers to specific path ending with the file name
     * the object is downloaded in single or multi chunks according to its size compared with config chunk size
     */
    context(CoroutineScope)
    @OptIn(InternalApi::class, InternalSdkApi::class)
    private fun downloadFile(fileSize: Long, from: S3Uri, to: String) {
        async {
            val toPath = Paths.get(to)
            // create the target directory if to path doesn't exist
            Files.createDirectories(toPath.parent)

            if (fileSize == 0L) { // for zero size file, can't chunk, so just download
                toPath.createFile()
                return@async
            }

            val chunkRanges = partition(fileSize, config.chunkSize)
            val writeChannel = File(to).writeChannel()
            chunkRanges.forEach {
                val contentRange = "bytes=${it.start}-${it.endInclusive}"
                val request = GetObjectRequest {
                    bucket = from.bucket
                    key = from.key
                    range = contentRange
                }
                s3.getObject(request) { resp ->
                    resp.body?.toReadChannel()?.copyTo(writeChannel, close = false)
                }
            }
            writeChannel.close()
        }
    }

    /**
     * copy a single object/directory from a S3 bucket to another S3 bucket
     * when from key refers to a specific source object, to key should be the copied object key
     * when from key refers to a key prefix of objects, to key should be the destination top directory of copied objects
     */
    context(CoroutineScope)
    @OptIn(InternalSdkApi::class)
    override fun copy(from: S3Uri, to: S3Uri, progressListener: ProgressListener?): Operation {
        val deferred = async {
            if (s3.headBucketOrNull(from.bucket) == null) {
                throw IllegalArgumentException("The source bucket does not exist or has no access to it")
            }
            if (s3.headBucketOrNull(to.bucket) == null) {
                throw IllegalArgumentException("The destination bucket does not exist or has no access to it")
            }

            if (!from.key.endsWith('/')) {
                val response = s3.headObjectOrNull(from)
                if (response != null) {
                    copyObject(response.contentLength, from, to)
                    return@async
                }
            }

            val keyPrefix = from.key.ensureEndsWith('/')

            val response = s3.listObjectsV2Paginated {
                bucket = from.bucket
                prefix = keyPrefix
            }

            if (response.firstOrNull()?.contents?.isNotEmpty() != true) {
                throw IllegalArgumentException("From S3 uri contains invalid key/keyPrefix")
            }

            response // Flow<ListObjectsV2Response>, a collection of pages
                .transform { it.contents?.forEach { obj -> emit(obj) } }
                .collect { obj ->
                    val key = obj.key!!
                    val subFrom = S3Uri(from.bucket, key)
                    val keySuffix = key.substringAfter(keyPrefix)
                    val subToKey = Paths.get(to.key, keySuffix).toString()
                    val subTo = S3Uri(to.bucket, subToKey)
                    copyObject(obj.size, subFrom, subTo)
                }
        }

        return DefaultOperation(deferred)
    }

    context(CoroutineScope)
    private fun copyObject(fileSize: Long, from: S3Uri, to: S3Uri) {
        // if file size <= chunkSize, call copyWholeObject
        // otherwise, call copyObjectParts
        if (fileSize <= config.chunkSize) {
            copyWholeObject(from, to)
        } else {
            copyObjectParts(fileSize, from, to)
        }
    }

    context(CoroutineScope)
    private fun copyWholeObject(from: S3Uri, to: S3Uri) {
        async {
            s3.copyObject {
                copySource = "${from.bucket}/${from.key}"
                bucket = to.bucket
                key = to.key
            }
        }
    }

    context(CoroutineScope)
    @OptIn(InternalSdkApi::class)
    private fun copyObjectParts(fileSize: Long, from: S3Uri, to: S3Uri) {
        async {
            val chunkRanges = partition(fileSize, config.chunkSize)

            val createMultipartCopyResponse = s3.createMultipartUpload {
                bucket = to.bucket
                key = to.key
            }
            val completedParts = mutableListOf<CompletedPart>()

            chunkRanges.forEachIndexed { index, chunkRange ->
                val contentRange = "bytes=${chunkRange.start}-${chunkRange.endInclusive}"
                val uploadPartCopyResponse = s3.uploadPartCopy {
                    copySource = "${from.bucket}/${from.key}"
                    bucket = to.bucket
                    key = to.key
                    copySourceRange = contentRange
                    partNumber = (index + 1)
                    uploadId = createMultipartCopyResponse.uploadId
                }
                val copyPartResult = uploadPartCopyResponse.copyPartResult!!
                completedParts.add(
                    CompletedPart {
                        eTag = copyPartResult.eTag
                        partNumber = (index + 1)
                    }
                )
            }

            s3.completeMultipartUpload {
                bucket = to.bucket
                key = to.key
                uploadId = createMultipartCopyResponse.uploadId
                multipartUpload { parts = completedParts }
            }
            writeChannel.close()
        }
    }
}

@InternalSdkApi
public suspend fun S3Client.headObjectOrNull(s3Uri: S3Uri): HeadObjectResponse? =
    try {
        // throw a not found exception if there's no such key object
        val response = headObject {
            bucket = s3Uri.bucket
            key = s3Uri.key
        }

        response
    } catch (_: NotFound) {
        null
    }

private suspend fun S3Client.headBucketOrNull(bucketName: String): HeadBucketResponse? =
    try { // first check if bucket exists
        val response = headBucket {
            bucket = bucketName
        }
        response
    } catch (e: S3Exception) {
        null
    }

@InternalSdkApi
public fun partition(fileSize: Long, chunkSize: Long): List<LongRange> =
    (0 until fileSize step chunkSize).map {
        it until minOf(it + chunkSize, fileSize)
    }

@InternalSdkApi
public fun String.ensureEndsWith(c: Char) = if (endsWith(c)) this else plus(c)

private fun ByteStream.toReadChannel(): SdkByteReadChannel = when (this) {
    is ByteStream.OneShotStream -> readFrom()
    is ByteStream.ReplayableStream -> newReader()
    is ByteStream.Buffer -> throw IllegalAccessError("In transfer manager, conversion from ByteStream to ByteBuffer shouldn't happen")
}
