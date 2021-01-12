/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.endpoint

/**
 * Resolves endpoints for a given service and region
 */
public interface EndpointResolver {

    /**
     * Resolve the [Endpoint] for the given service and region
     */
    public suspend fun resolve(service: String, region: String): Endpoint
}
