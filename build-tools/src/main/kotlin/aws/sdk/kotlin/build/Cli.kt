/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build

import aws.sdk.kotlin.build.cmd.Scaffold
import kotlinx.cli.*

@OptIn(ExperimentalCli::class)
class Cli {
    companion object {
        // execute a build command
        fun exec(args: Array<String>): Cli = Cli().apply { parse(args) }
    }

    private val parser = ArgParser("build-tools")

    // due to the nature of subcommands simply parsing will run the commands execute()
    fun parse(args: Array<String>) {
        val scaffold = Scaffold()
        parser.subcommands(scaffold)
        parser.parse(args)
    }
}
