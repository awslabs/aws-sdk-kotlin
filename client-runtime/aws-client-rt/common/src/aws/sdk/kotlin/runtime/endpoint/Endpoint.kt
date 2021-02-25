/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.endpoint

/**
 * Represents the endpoint a service client should make API operation calls to.
 *
 * The SDK will automatically resolve these endpoints per API client using
 * an internal resolver.
 *
 * @property hostname The base URL endpoint clients will use to make API calls to e.g. "{service-id}.{region}.amazonaws.com"
 * @property protocol The protocol to use when making a connection e.g. "HTTPS"
 * @property port The port to connect to when making requests to this endpoint. When not specified the default port dictated
 * by the protocol will be used.

 * @property isHostnameImmutable Flag indicating that the hostname can be modified by the SDK client.
 *
 * If the hostname is mutable the SDK clients may modify any part of the hostname based
 * on the requirements of the API (e.g. adding or removing content in the hostname).
 *
 * As an example Amazon S3 Client prefixing "bucketname" to the hostname or changing th hostname
 * service name component from "s3" to "s3-accespoint.dualstack." requires mutable hostnames.
 *
 * Care should be taken when setting this flag and providing a custom endpoint. If the hostname
 * is expected to be mutable and the client cannot modify the endpoint correctly, the operation
 * will likely fail.

 * @property signingName The service name that should be used for signing requests to this endpoint. This overrides the default
 * signing name used by an SDK client.

 * @property signingRegion The region that should be used for signing requests to this endpoint. This overrides the default
 * signing region used by an SDK client.
 */
public data class Endpoint(
    public val hostname: String,
    public val protocol: String,
    public val port: Int? = null,
    public val isHostnameImmutable: Boolean = false,
    public val signingName: String? = null,
    public val signingRegion: String? = null
)
