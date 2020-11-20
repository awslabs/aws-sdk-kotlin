/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.service.s3.transform

import com.amazonaws.service.s3.model.GetObjectResponse
import software.aws.clientrt.http.feature.DeserializationProvider
import software.aws.clientrt.http.feature.HttpDeserialize
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.http.toByteStream


class GetObjectResponseDeserializer: HttpDeserialize {
    override suspend fun deserialize(response: HttpResponse, provider: DeserializationProvider): GetObjectResponse {
        return GetObjectResponse{
             deleteMarker = response.headers["x-amz-delete-marker"]?.toBoolean()
             acceptRanges = response.headers["accept-ranges"]
             expiration = response.headers["x-amz-expiration"]
             restore = response.headers["x-amz-restore"]
             lastModified = response.headers["Last-Modified"]
             contentLength = response.headers["Content-Length"]?.toLong()
             eTag = response.headers["ETag"]
             missingMeta = response.headers["x-amz-missing-meta"]?.toLong()
             versionId = response.headers["x-amz-version-id"]
             cacheControl = response.headers["Cache-Control"]
             contentDisposition = response.headers["Content-Disposition"]
             contentEncoding = response.headers["Content-Encoding"]
             contentLanguage = response.headers["Content-Language"]
             contentRange = response.headers["Content-Range"]
             contentType = response.headers["Content-Type"]
             expires = response.headers["Expires"]
             websiteRedirectLocation = response.headers["x-amz-website-redirect-location"]
             serverSideEncryption = response.headers["x-amz-server-side-encryption"]
             sseCustomerAlgorithm = response.headers["x-amz-server-side-encryption-customer-algorithm"]
             sseCustomerKeyMd5 = response.headers["x-amz-server-side-encryption-customer-key-MD5"]
             ssekmsKeyId = response.headers["x-amz-server-side-encryption-aws-kms-key-id"]
             storageClass = response.headers["x-amz-storage-class"]
             requestCharged = response.headers["x-amz-request-charged"]
             replicationStatus = response.headers["x-amz-replication-status"]
             partsCount = response.headers["x-amz-mp-parts-count"]?.toLong()
             tagCount = response.headers["x-amz-tagging-count"]?.toLong()
             objectLockMode = response.headers["x-amz-object-lock-mode"]
             objectLockRetainUntilDate = response.headers["x-amz-object-lock-retain-until-date"]
             objectLockLegalHoldStatus = response.headers["x-amz-object-lock-legal-hold"]
             body = response.body.toByteStream()
        }
    }
}
