/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import software.aws.clientrt.content.ByteStream
import software.aws.clientrt.content.fromFile
import java.io.File
import java.nio.file.Files

const val bucketName = "s3-media-ingestion-example"
const val ingestionDirPath = "/tmp/media-in"
const val completedDirPath = "/tmp/media-processed"
const val failedDirPath = "/tmp/media-failed"

// media metadata is extracted from filename: <title>_<year>.avi
val filenameMetadataRegex = "([\\w\\s]+)_([\\d]+).avi".toRegex()

/**
 * This program reads media files from a specified directory and uploads media files to S3
 */
fun main() = runBlocking {
    val client = S3Client { region = "us-east-2" }

    try {
        validateS3(client)
        listOf(completedDirPath, failedDirPath).forEach { validateDirectory(it) }
        val ingestionDir = validateDirectory(ingestionDirPath)

        val uploadResults = ingestionDir
            .walk().asFlow()
            .mapNotNull(::mediaMetadataExtractor)
            .map { mediaMetadata ->
                uploadToS3(client, mediaMetadata)
            }
            .toList()

        moveFiles(uploadResults)

        val (successes, failures) = uploadResults.partition { it is Success }
        when (failures.isEmpty()) {
            true -> println("Media uploaded successfully: $successes")
            false -> println("Successfully uploaded: $successes \nFailed to upload: $failures")
        }
    } finally {
        client.close()
    }
}

// Check for valid S3 configuration based on account
suspend fun validateS3(s3Client: S3Client) {
    val listBucketResponse = s3Client.listBuckets(ListBucketsRequest { })
    check(listBucketResponse.buckets?.any { it.name == bucketName } ?: false) { "Bucket $bucketName does not exist" }
}

// Move files to directories based on upload results
fun moveFiles(uploadResults: List<UploadResult>) =
    uploadResults
        .map { uploadResult -> uploadResult.mediaMetadata.file.toPath() to (uploadResult is Success) }
        .forEach { (file, uploadSuccess) ->
            val targetFilePath = if (uploadSuccess) completedDirPath else failedDirPath
            val targetPath = File(targetFilePath)
            Files.move(file, File(targetPath, file.fileName.toString()).toPath())
        }

// Classes for S3 upload results
sealed class UploadResult { abstract val mediaMetadata: MediaMetadata }
data class Success(val location: String, override val mediaMetadata: MediaMetadata) : UploadResult()
data class UploadError(val error: Throwable, override val mediaMetadata: MediaMetadata) : UploadResult()
data class Failure(val reason: String, override val mediaMetadata: MediaMetadata) : UploadResult()

// Upload to S3 if file not already uploaded
suspend fun uploadToS3(s3Client: S3Client, mediaMetadata: MediaMetadata): UploadResult {
    val existsInS3 = s3Client
        .listObjects(ListObjectsRequest { bucket = bucketName })
        .contents?.any { it.key == mediaMetadata.s3KeyName } ?: false

    if (existsInS3) return Failure("${mediaMetadata.s3KeyName} already uploaded.", mediaMetadata)

    return try {
        s3Client.putObject(
            PutObjectRequest {
                bucket = bucketName
                key = mediaMetadata.s3KeyName
                body = ByteStream.fromFile(mediaMetadata.file)
                metadata = mediaMetadata.toMap()
            }
        )
        Success("$bucketName/${mediaMetadata.s3KeyName}", mediaMetadata)
    } catch (e: Exception) {
        UploadError(e, mediaMetadata)
    }
}

// Classes, properties, and functions for media metadata
data class MediaMetadata(val title: String, val year: Int, val file: File)
val MediaMetadata.s3KeyName get() = "$title-$year"
fun MediaMetadata.toMap() = mapOf("title" to title, "year" to year.toString())
fun mediaMetadataExtractor(file: File): MediaMetadata? {
    if (!file.isFile || file.length() == 0L) return null

    val matchResult = filenameMetadataRegex.find(file.name) ?: return null

    val (title, year) = matchResult.destructured
    return MediaMetadata(title, year.toInt(), file)
}

// Validate file path and optionally create directory
fun validateDirectory(dirPath: String): File {
    val dir = File(dirPath)

    require(dir.isDirectory || !dir.exists()) { "Unable to use $dir" }

    if (!dir.exists()) require(dir.mkdirs()) { "Unable to create $dir" }

    return dir
}
