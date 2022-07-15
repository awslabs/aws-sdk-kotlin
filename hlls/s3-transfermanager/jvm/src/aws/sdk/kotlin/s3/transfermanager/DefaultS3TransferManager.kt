package aws.sdk.kotlin.s3.transfermanager

import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.s3.transfermanager.handler.DefaultOperation
import aws.sdk.kotlin.s3.transfermanager.handler.Operation
import aws.sdk.kotlin.s3.transfermanager.listener.ProgressListener
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Response
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.fromFile
import aws.smithy.kotlin.runtime.content.writeToFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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

            if (localFile.isFile()) {
                uploadFile(localFile, to)
            } else if (localFile.isDirectory()) {
                uploadDirectory(localFile, to, progressListener)
            } else {
                throw IllegalArgumentException("From path is invalid")
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
            uploadWholeFile(localFile, to)
        } else { // for large file over config chunk size
            uploadFileParts(localFile, to)
        }
    }

    context(CoroutineScope)
    private fun uploadWholeFile(localFile: File, to: S3Uri) {
        async<Unit> {
            s3.putObject {
                bucket = to.bucket
                key = to.key
                body = ByteStream.fromFile(localFile)
            }
        }
    }

    context(CoroutineScope)
    private fun uploadFileParts(localFile: File, to: S3Uri) {
        async<Unit> {
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
     */
    context(CoroutineScope)
    override fun download(from: S3Uri, to: String, progressListener: ProgressListener?): Operation {
        // in S3 system, try to use S3.HeadObject(s3Path.key) first to see whether or not there is
        // a real key object there, if not exception is thrown, we can call downloadFile() to download it locally
        // then call s3.ListObjects(from.key) to get a List of object with such key-prefix and
        // then try to re-serialize the tree directory locally if list is not empty by calling uploadDirectory()
        // (may need to add '/' or '\' at the end of key-prefix to limit it's precision)


        // if given /, then just check if there's some directory in S3 without checking single object
        // if not given /, need to check both directory and single object
        // should first head object whenever
        val deferred = async {
            val localFile = File(to)
            // throw IllegalArgumentException if from path is invalid
            require(localFile.isDirectory()) { "To path is not a valid directory" }
            require(from.key[from.key.length - 1] != '/') { "From key can not end with /" } // can end with /
            // check if the current key is a keyPrefix

            var keyPrefix = from.key.plus('/') // assume key is a directory ending with /
            val response = s3.listObjectsV2 {
                bucket = from.bucket
                prefix = keyPrefix
            }
            if (response.keyCount > 0) {
                downloadDirectory(response, localFile, progressListener)
            }

            // assume key is just an object's key
            try {
                // throw a not found exception if there's no such key object
                s3.headObject {
                    bucket = from.bucket
                    key = from.key
                }

                // if object exist, download that object
                val subTo = localFile.toPath().resolve(from.key.substringAfterLast('/')).toString()
                downLoadFile(from, subTo)
            } catch (e: Exception) {
//                e.printStackTrace()
            }
        }

        return DefaultOperation(deferred)
    }

    context(CoroutineScope)
    private fun downLoadFile(from: S3Uri, to: String) {
        async {
            val request = GetObjectRequest {
                bucket = from.bucket
                key = from.key
            }
            s3.getObject(request) { resp ->
                // resp is valid until the end of the block
                // do not attempt to store or process the stream after the block returns

                // resp.body is of type ByteStream
                val rc = resp.body?.writeToFile(Paths.get(to))
                rc
            }
        }
    }

    context(CoroutineScope)
    private fun downloadDirectory(response: ListObjectsV2Response, localFile: File, progressListener: ProgressListener?) {
//     for download directory, in S3, go to s3Path directory, locally go to fileDirectory
//     first strip off the bottom key-prefix directory, if the current folder doesn't exist,
//     then create that folder under current filePath
//     then iterate through key-prefix's all sub-file via s3.ListObjects(), concatenate a longer prefix using prefix
//     chunk after the current prefix's end '/'
//     and call download() in each loop using their longer prefix and current level directory
//     to recursively execute single object download or sub-directory download at local next level
//
//     ideal result is like this
//     S3:
//     bucket/path/to/a/key
//
//     bucket/path/to/a/key/file1
//     bucket/path/to/a/key     dir1/file2
//     bucket/path/to/a/key/dir1/file3
//     bucket/path/to/a/key/bar/file4
//     bucket/path/to/a/key/bar/dir1/file5
//
//
//     Files system:
//     /foo/bar/key/
//
//     /foo/bar/key/file1
//     /foo/bar/key/dir1/file2
//     /foo/bar/key/dir1/file3
//     /foo/bar/key/bar/file4
//     /foo/bar/key/bar/dir1/file5



        // replace complex subFrom String operation ,
        // play with mkdir() which can create different level's directories, and download all subObjects in one level
        // for loop under these different directories


        async {
            response.contents?.forEach {
                var subFrom = response.prefix?.let { it1 -> it.key?.substringAfter(it1) }
                if (subFrom != null) {
                    if (subFrom.startsWith('/')) {
                        subFrom = subFrom.substringAfter('/')
                    }
                    subFrom = subFrom.substringBefore('/')
                    val subTo = localFile.toPath().resolve(subFrom).toString()

                    var subPrefix = Paths.get(response.prefix, subFrom).toString() + '/'

                    // see if current subPrefix refers to a directory in S3
                    val subResponse = s3.listObjectsV2 {
                        bucket = response.name
                        prefix = subPrefix
                    }
                    if (subResponse.keyCount > 0) {
                        // only create and dig into subTo directory download when first meet it
                        if (!Paths.get(subTo).toFile().exists()) {
                            val subLocalFile = Files.createDirectories(Paths.get(subTo)).toFile()
                            downloadDirectory(subResponse, subLocalFile, progressListener)
                        }
                    }

                    // see if current subPrefix refers to an object
                    subPrefix = subPrefix.substringBeforeLast('/')
                    try {
                        s3.headObject {
                            bucket = response.name
                            key = subPrefix
                        }

                        // if object exist, download that object
                        response.name?.let { it1 -> S3Uri(it1, subPrefix) }?.let { it2 -> downLoadFile(it2, subTo) }
                    } catch (e: Exception) {
//                        e.printStackTrace()
//                        s3.listObjectsV2Paginated()
                    }
                }
            }
        }
    }

    override suspend fun copy(from: List<S3Uri>, to: S3Uri, progressListener: ProgressListener?): Operation {
        TODO("Not yet implemented")
    }
}
