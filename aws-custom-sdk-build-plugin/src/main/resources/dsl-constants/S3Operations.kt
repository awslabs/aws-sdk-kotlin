/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk.constants

/**
 * Type-safe operation constants for S3 service.
 * 
 * These constants can be used in the AWS Custom SDK Build plugin DSL
 * to specify which operations to include in custom client builds.
 * 
 * Generated during SDK build process.
 */
object S3Operations {
/**
 * Operation: AbortMultipartUpload
 */
const val AbortMultipartUpload = "AbortMultipartUpload"
/**
 * Operation: CompleteMultipartUpload
 */
const val CompleteMultipartUpload = "CompleteMultipartUpload"
/**
 * Operation: CopyObject
 */
const val CopyObject = "CopyObject"
/**
 * Operation: CreateBucket
 */
const val CreateBucket = "CreateBucket"
/**
 * Operation: CreateBucketMetadataTableConfiguration
 */
const val CreateBucketMetadataTableConfiguration = "CreateBucketMetadataTableConfiguration"
/**
 * Operation: CreateMultipartUpload
 */
const val CreateMultipartUpload = "CreateMultipartUpload"
/**
 * Operation: CreateSession
 */
const val CreateSession = "CreateSession"
/**
 * Operation: DeleteBucket
 */
const val DeleteBucket = "DeleteBucket"
/**
 * Operation: DeleteBucketAnalyticsConfiguration
 */
const val DeleteBucketAnalyticsConfiguration = "DeleteBucketAnalyticsConfiguration"
/**
 * Operation: DeleteBucketCors
 */
const val DeleteBucketCors = "DeleteBucketCors"
/**
 * Operation: DeleteBucketEncryption
 */
const val DeleteBucketEncryption = "DeleteBucketEncryption"
/**
 * Operation: DeleteBucketIntelligentTieringConfiguration
 */
const val DeleteBucketIntelligentTieringConfiguration = "DeleteBucketIntelligentTieringConfiguration"
/**
 * Operation: DeleteBucketInventoryConfiguration
 */
const val DeleteBucketInventoryConfiguration = "DeleteBucketInventoryConfiguration"
/**
 * Operation: DeleteBucketLifecycle
 */
const val DeleteBucketLifecycle = "DeleteBucketLifecycle"
/**
 * Operation: DeleteBucketMetadataTableConfiguration
 */
const val DeleteBucketMetadataTableConfiguration = "DeleteBucketMetadataTableConfiguration"
/**
 * Operation: DeleteBucketMetricsConfiguration
 */
const val DeleteBucketMetricsConfiguration = "DeleteBucketMetricsConfiguration"
/**
 * Operation: DeleteBucketOwnershipControls
 */
const val DeleteBucketOwnershipControls = "DeleteBucketOwnershipControls"
/**
 * Operation: DeleteBucketPolicy
 */
const val DeleteBucketPolicy = "DeleteBucketPolicy"
/**
 * Operation: DeleteBucketReplication
 */
const val DeleteBucketReplication = "DeleteBucketReplication"
/**
 * Operation: DeleteBucketTagging
 */
const val DeleteBucketTagging = "DeleteBucketTagging"
/**
 * Operation: DeleteBucketWebsite
 */
const val DeleteBucketWebsite = "DeleteBucketWebsite"
/**
 * Operation: DeleteObject
 */
const val DeleteObject = "DeleteObject"
/**
 * Operation: DeleteObjectTagging
 */
const val DeleteObjectTagging = "DeleteObjectTagging"
/**
 * Operation: DeleteObjects
 */
const val DeleteObjects = "DeleteObjects"
/**
 * Operation: DeletePublicAccessBlock
 */
const val DeletePublicAccessBlock = "DeletePublicAccessBlock"
/**
 * Operation: GetBucketAccelerateConfiguration
 */
const val GetBucketAccelerateConfiguration = "GetBucketAccelerateConfiguration"
/**
 * Operation: GetBucketAcl
 */
const val GetBucketAcl = "GetBucketAcl"
/**
 * Operation: GetBucketAnalyticsConfiguration
 */
const val GetBucketAnalyticsConfiguration = "GetBucketAnalyticsConfiguration"
/**
 * Operation: GetBucketCors
 */
const val GetBucketCors = "GetBucketCors"
/**
 * Operation: GetBucketEncryption
 */
const val GetBucketEncryption = "GetBucketEncryption"
/**
 * Operation: GetBucketIntelligentTieringConfiguration
 */
const val GetBucketIntelligentTieringConfiguration = "GetBucketIntelligentTieringConfiguration"
/**
 * Operation: GetBucketInventoryConfiguration
 */
const val GetBucketInventoryConfiguration = "GetBucketInventoryConfiguration"
/**
 * Operation: GetBucketLifecycleConfiguration
 */
const val GetBucketLifecycleConfiguration = "GetBucketLifecycleConfiguration"
/**
 * Operation: GetBucketLocation
 */
const val GetBucketLocation = "GetBucketLocation"
/**
 * Operation: GetBucketLogging
 */
const val GetBucketLogging = "GetBucketLogging"
/**
 * Operation: GetBucketMetadataTableConfiguration
 */
const val GetBucketMetadataTableConfiguration = "GetBucketMetadataTableConfiguration"
/**
 * Operation: GetBucketMetricsConfiguration
 */
const val GetBucketMetricsConfiguration = "GetBucketMetricsConfiguration"
/**
 * Operation: GetBucketNotificationConfiguration
 */
const val GetBucketNotificationConfiguration = "GetBucketNotificationConfiguration"
/**
 * Operation: GetBucketOwnershipControls
 */
const val GetBucketOwnershipControls = "GetBucketOwnershipControls"
/**
 * Operation: GetBucketPolicy
 */
const val GetBucketPolicy = "GetBucketPolicy"
/**
 * Operation: GetBucketPolicyStatus
 */
const val GetBucketPolicyStatus = "GetBucketPolicyStatus"
/**
 * Operation: GetBucketReplication
 */
const val GetBucketReplication = "GetBucketReplication"
/**
 * Operation: GetBucketRequestPayment
 */
const val GetBucketRequestPayment = "GetBucketRequestPayment"
/**
 * Operation: GetBucketTagging
 */
const val GetBucketTagging = "GetBucketTagging"
/**
 * Operation: GetBucketVersioning
 */
const val GetBucketVersioning = "GetBucketVersioning"
/**
 * Operation: GetBucketWebsite
 */
const val GetBucketWebsite = "GetBucketWebsite"
/**
 * Operation: GetObject
 */
const val GetObject = "GetObject"
/**
 * Operation: GetObjectAcl
 */
const val GetObjectAcl = "GetObjectAcl"
/**
 * Operation: GetObjectAttributes
 */
const val GetObjectAttributes = "GetObjectAttributes"
/**
 * Operation: GetObjectLegalHold
 */
const val GetObjectLegalHold = "GetObjectLegalHold"
/**
 * Operation: GetObjectLockConfiguration
 */
const val GetObjectLockConfiguration = "GetObjectLockConfiguration"
/**
 * Operation: GetObjectRetention
 */
const val GetObjectRetention = "GetObjectRetention"
/**
 * Operation: GetObjectTagging
 */
const val GetObjectTagging = "GetObjectTagging"
/**
 * Operation: GetObjectTorrent
 */
const val GetObjectTorrent = "GetObjectTorrent"
/**
 * Operation: GetPublicAccessBlock
 */
const val GetPublicAccessBlock = "GetPublicAccessBlock"
/**
 * Operation: HeadBucket
 */
const val HeadBucket = "HeadBucket"
/**
 * Operation: HeadObject
 */
const val HeadObject = "HeadObject"
/**
 * Operation: ListBucketAnalyticsConfigurations
 */
const val ListBucketAnalyticsConfigurations = "ListBucketAnalyticsConfigurations"
/**
 * Operation: ListBucketIntelligentTieringConfigurations
 */
const val ListBucketIntelligentTieringConfigurations = "ListBucketIntelligentTieringConfigurations"
/**
 * Operation: ListBucketInventoryConfigurations
 */
const val ListBucketInventoryConfigurations = "ListBucketInventoryConfigurations"
/**
 * Operation: ListBucketMetricsConfigurations
 */
const val ListBucketMetricsConfigurations = "ListBucketMetricsConfigurations"
/**
 * Operation: ListBuckets
 */
const val ListBuckets = "ListBuckets"
/**
 * Operation: ListDirectoryBuckets
 */
const val ListDirectoryBuckets = "ListDirectoryBuckets"
/**
 * Operation: ListMultipartUploads
 */
const val ListMultipartUploads = "ListMultipartUploads"
/**
 * Operation: ListObjectVersions
 */
const val ListObjectVersions = "ListObjectVersions"
/**
 * Operation: ListObjects
 */
const val ListObjects = "ListObjects"
/**
 * Operation: ListObjectsV2
 */
const val ListObjectsV2 = "ListObjectsV2"
/**
 * Operation: ListParts
 */
const val ListParts = "ListParts"
/**
 * Operation: PutBucketAccelerateConfiguration
 */
const val PutBucketAccelerateConfiguration = "PutBucketAccelerateConfiguration"
/**
 * Operation: PutBucketAcl
 */
const val PutBucketAcl = "PutBucketAcl"
/**
 * Operation: PutBucketAnalyticsConfiguration
 */
const val PutBucketAnalyticsConfiguration = "PutBucketAnalyticsConfiguration"
/**
 * Operation: PutBucketCors
 */
const val PutBucketCors = "PutBucketCors"
/**
 * Operation: PutBucketEncryption
 */
const val PutBucketEncryption = "PutBucketEncryption"
/**
 * Operation: PutBucketIntelligentTieringConfiguration
 */
const val PutBucketIntelligentTieringConfiguration = "PutBucketIntelligentTieringConfiguration"
/**
 * Operation: PutBucketInventoryConfiguration
 */
const val PutBucketInventoryConfiguration = "PutBucketInventoryConfiguration"
/**
 * Operation: PutBucketLifecycleConfiguration
 */
const val PutBucketLifecycleConfiguration = "PutBucketLifecycleConfiguration"
/**
 * Operation: PutBucketLogging
 */
const val PutBucketLogging = "PutBucketLogging"
/**
 * Operation: PutBucketMetricsConfiguration
 */
const val PutBucketMetricsConfiguration = "PutBucketMetricsConfiguration"
/**
 * Operation: PutBucketNotificationConfiguration
 */
const val PutBucketNotificationConfiguration = "PutBucketNotificationConfiguration"
/**
 * Operation: PutBucketOwnershipControls
 */
const val PutBucketOwnershipControls = "PutBucketOwnershipControls"
/**
 * Operation: PutBucketPolicy
 */
const val PutBucketPolicy = "PutBucketPolicy"
/**
 * Operation: PutBucketReplication
 */
const val PutBucketReplication = "PutBucketReplication"
/**
 * Operation: PutBucketRequestPayment
 */
const val PutBucketRequestPayment = "PutBucketRequestPayment"
/**
 * Operation: PutBucketTagging
 */
const val PutBucketTagging = "PutBucketTagging"
/**
 * Operation: PutBucketVersioning
 */
const val PutBucketVersioning = "PutBucketVersioning"
/**
 * Operation: PutBucketWebsite
 */
const val PutBucketWebsite = "PutBucketWebsite"
/**
 * Operation: PutObject
 */
const val PutObject = "PutObject"
/**
 * Operation: PutObjectAcl
 */
const val PutObjectAcl = "PutObjectAcl"
/**
 * Operation: PutObjectLegalHold
 */
const val PutObjectLegalHold = "PutObjectLegalHold"
/**
 * Operation: PutObjectLockConfiguration
 */
const val PutObjectLockConfiguration = "PutObjectLockConfiguration"
/**
 * Operation: PutObjectRetention
 */
const val PutObjectRetention = "PutObjectRetention"
/**
 * Operation: PutObjectTagging
 */
const val PutObjectTagging = "PutObjectTagging"
/**
 * Operation: PutPublicAccessBlock
 */
const val PutPublicAccessBlock = "PutPublicAccessBlock"
/**
 * Operation: RenameObject
 */
const val RenameObject = "RenameObject"
/**
 * Operation: RestoreObject
 */
const val RestoreObject = "RestoreObject"
/**
 * Operation: SelectObjectContent
 */
const val SelectObjectContent = "SelectObjectContent"
/**
 * Operation: UploadPart
 */
const val UploadPart = "UploadPart"
/**
 * Operation: UploadPartCopy
 */
const val UploadPartCopy = "UploadPartCopy"
/**
 * Operation: WriteGetObjectResponse
 */
const val WriteGetObjectResponse = "WriteGetObjectResponse"
}