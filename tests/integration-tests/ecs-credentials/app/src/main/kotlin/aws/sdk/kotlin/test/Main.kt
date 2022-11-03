/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.test

import aws.sdk.kotlin.runtime.auth.credentials.EcsCredentialsProvider
import aws.sdk.kotlin.services.sts.StsClient
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    StsClient {
        region = "us-east-2"
        credentialsProvider = EcsCredentialsProvider()
    }.use { client ->
        val resp = client.getCallerIdentity()

        println("Account: ${resp.account}")
        println("UserID: ${resp.userId}")
        println("ARN: ${resp.arn}")
    }
}
