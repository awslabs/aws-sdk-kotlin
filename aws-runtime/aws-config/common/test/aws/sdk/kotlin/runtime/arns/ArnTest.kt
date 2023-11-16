/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.arns

import kotlin.test.Test
import kotlin.test.assertEquals

class ArnTest {

    @Test
    fun testParse() {
        val tests = listOf(
            "arn:aws:iam::123456789012:user/johndoe" to Arn {
                partition = "aws"
                service = "iam"
                accountId = "123456789012"
                resource = "user/johndoe"
            },
            "arn:aws:sns:us-east-1:123456789012:example-sns-topic-name" to Arn {
                partition = "aws"
                service = "sns"
                region = "us-east-1"
                accountId = "123456789012"
                resource = "example-sns-topic-name"
            },
            "arn:aws:ec2:us-east-1:123456789012:vpc/vpc-0e9801d129EXAMPLE" to Arn {
                partition = "aws"
                service = "ec2"
                region = "us-east-1"
                accountId = "123456789012"
                resource = "vpc/vpc-0e9801d129EXAMPLE"
            },
            "arn:aws:s3:::bucket/key" to Arn {
                partition = "aws"
                service = "s3"
                resource = "bucket/key"
            },
            "arn:aws:lambda:us-east-2:12345:function" to Arn {
                partition = "aws"
                service = "lambda"
                region = "us-east-2"
                accountId = "12345"
                resource = "function"
            },
            "arn:aws:lambda:us-east-2:12345:function:version" to Arn {
                partition = "aws"
                service = "lambda"
                region = "us-east-2"
                accountId = "12345"
                resource = "function:version"
            },
        )

        tests.forEach { (arnString, arnExpected) ->
            val parsed = Arn.parse(arnString)
            assertEquals(arnExpected, parsed)
            // test round trip
            assertEquals(arnString, parsed.toString())
        }
    }

    @Test
    fun testEquivalence() {
        val arn = "arn:aws:s3:us-east-1:12345678910:myresource:foobar"
        val arn1 = Arn.parse(arn)
        val arn2 = Arn.parse(arn)
        assertEquals(arn1, arn2)
    }
}
