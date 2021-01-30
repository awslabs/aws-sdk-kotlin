/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build

/**
 * aws-sdk-kotlin build utilities.
 *
 * usage:
 * ./gradlew -q :build-tools:run --args="subcommand [options] [args]"
 */
fun main(args: Array<String>) {
    Cli.exec(args)
}
