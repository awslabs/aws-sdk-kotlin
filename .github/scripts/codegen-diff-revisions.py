#!/usr/bin/env python3
#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
#
# This script can be run and tested locally. To do so, you should check out
# a second aws-sdk-kotlin repository so that you can work on the script and still
# run it without it immediately bailing for an unclean working tree (and to avoid mutating
# your local repository).
#
# Example:
# `aws-sdk-kotlin/` - the main repo you're working out of <path-to-working-dir> below
# `/tmp/aws-sdk-kotlin/` - the repo you're testing against
#
# Generate code using the current HEAD of the repository.

# ```
# $ cd /tmp/aws-sdk-kotlin
# $ <path-to-working-dir>/.github/scripts/codegen-diff-revisions.py codegen --bootstrap +s3,+dynamodb
# ```
#
#
# Generate diffs
#
# ```
# $ cd /tmp/aws-sdk-kotlin
# HEAD_SHA=$(git rev-parse <my-head-sha>)
# BASE_SHA=$(git rev-parse <my-base-branch>)
# HEAD_BRANCH_NAME="__tmp-localonly-head"
# BASE_BRANCH_NAME="__tmp-localonly-base"
#
# <path-to-working-dir>/.github/scripts/codegen-diff-revisions.py --head-sha $HEAD_SHA generate-diffs --base-sha $BASE_SHA $BASE_BRANCH_NAME $HEAD_BRANCH_NAME
# ```
#
# This script requires `difftags` to be installed from the smithy-rs repository. This requires installing a working
# rust toolchain (cargo, rustc, etc).
# ```
# curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`
# ```
#
# Install difftags from smithy-rs
# ```
# git clone --depth 1 https://github.com/smithy-lang/smithy-rs.git
# cd smithy-rs/tools/ci-build/difftags
# cargo install --path .
# difftags -h
# ```
# Make sure the local version matches the version referenced from the GitHub Actions workflow.

import argparse
import os
import sys
import subprocess
import tempfile
import shlex

OUTPUT_PATH = "tmp-codegen-diff"

COMMIT_AUTHOR_NAME = "GitHub Action (generated codegen diff)"
COMMIT_AUTHOR_EMAIL = "generated-code-action@github.com"

CDN_URL = "https://d3l30fr4k4zcv0.cloudfront.net"


def eprint(*args, **kwargs):
    """
    Prints to stderr
    """
    print(*args, file=sys.stderr, **kwargs)


def running_in_github_action():
    """
    Test if currently running in a GitHub action or running locally
    :return: True if running in GH, False otherwise
    """
    return "GITHUB_WORKFLOW" in os.environ


def run(command, shell=False):
    """
    Run a command
    :param command: command to run
    :param shell: Flag indicating if shell should be used by subprocess command
    """
    if not shell:
        command = shlex.split(command)
    subprocess.run(command, stdout=sys.stderr, stderr=sys.stderr, shell=shell, check=True)


def get_cmd_output(command):
    """
    Returns the output from a shell command. Bails if the command failed

    :param command: command to run
    :return: output from running the given command
    """
    result = subprocess.run(shlex.split(command), capture_output=True, check=True)
    return result.stdout.decode("utf-8").strip()


def get_cmd_status(command):
    """
    Runs a shell command and returns its exit status

    :param command: command to run
    :return: exit status of the command
    """
    return subprocess.run(command, capture_output=True, shell=True).returncode


def generate_and_commit_generated_code(sha, services_to_bootstrap):
    """
    Generate codegen output and commit it
    :param sha: The commit SHA being generated
    :param services_to_bootstrap: list of services to pass on to codegen bootstrap
    :return:
    """
    run(f'./gradlew --rerun-tasks :codegen:sdk:bootstrap -Paws.services={services_to_bootstrap}')
    run(f"rm -rf {OUTPUT_PATH}")
    run(f"mkdir {OUTPUT_PATH}")
    run(f'cp -r services {OUTPUT_PATH}')
    run(f'git add -f {OUTPUT_PATH}')
    run(f"git -c 'user.name={COMMIT_AUTHOR_NAME}' -c 'user.email={COMMIT_AUTHOR_EMAIL}' commit --no-verify -m 'Generated code for {sha}' --allow-empty")


def make_diff(opts, title, path_to_diff, outsubdir, ignore_whitespace):
    base_sha = opts.base_sha
    head_sha = opts.head_sha
    ws_flag = "-b" if ignore_whitespace else ""
    diff_exists = get_cmd_status(f"git diff --quiet {ws_flag} {opts.base_branch} {opts.head_branch} -- {path_to_diff}")

    if diff_exists == 0:
        eprint(f"No diff output for {base_sha}..{head_sha}")
        return None

    dest_path = f"{base_sha}/{head_sha}/{outsubdir}"
    run(f'mkdir -p {OUTPUT_PATH}/{dest_path}')
    whitespace_context = "(ignoring whitespace)" if ignore_whitespace else ""

    with tempfile.NamedTemporaryFile(mode='w') as tmp_file:
        diff_cmd = f"git diff -U30 {ws_flag} {opts.base_branch} {opts.head_branch} -- {path_to_diff}"
        eprint(f"Running diff cmd: {diff_cmd}")
        output = get_cmd_output(diff_cmd)
        tmp_file.write(output)

        subtitle = f"rev. {base_sha}..{head_sha} {whitespace_context}"
        generate_html_cmd = f"difftags --title \"{title}\" --subtitle \"{subtitle}\" --output-dir {OUTPUT_PATH}/{dest_path} {tmp_file.name}"
        eprint(f"Running generate html cmd: {generate_html_cmd}")
        run(generate_html_cmd)

    return dest_path


def diff_link(diff_text, empty_diff_text, diff_location, alternate_text, alternate_location):
    if diff_location is None:
        return empty_diff_text

    return f"[{diff_text}]({CDN_URL}/codegen-diff/{diff_location}/index.html) ([{alternate_text}]({CDN_URL}/codegen-diff/{alternate_location}/index.html))"


def make_diffs(opts):
    path_to_diff = f"{OUTPUT_PATH}/services"

    sdk_ws = make_diff(opts, 'AWS SDK', path_to_diff, "aws-sdk", ignore_whitespace=False)
    sdk_no_ws = make_diff(opts, 'AWS SDK', path_to_diff, "aws-sdk-ignore-ws", ignore_whitespace=True)
    sdk_links = diff_link('AWS SDK', 'No codegen difference in the AWS SDK', sdk_ws, 'ignoring whitespace', sdk_no_ws)

    return f'A new generated diff is ready to view.\\n\\n- {sdk_links}\\n'


def _codegen_cmd(opts):
    generate_and_commit_generated_code(opts.head_sha, opts.bootstrap)


def _generate_diffs_cmd(opts):
    bot_message = make_diffs(opts)
    with open(f"{OUTPUT_PATH}/bot-message", 'w') as f:
        f.write(bot_message)


def create_cli():
    parser = argparse.ArgumentParser(
        prog="codegen-diff-revisions",
        description="Generate HTML diffs of codegen output",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )

    parser.add_argument("--head-sha", help="head commit to use (defaults to whatever current HEAD) is")

    subparsers = parser.add_subparsers()
    codegen = subparsers.add_parser("codegen", help="generate and commit generated code")
    codegen.add_argument("--bootstrap", help="services to pass to bootstrap and include in diff output",
                         default="+dynamodb,+codebuild,+sts,+ec2,+polly,+s3")
    codegen.set_defaults(cmd=_codegen_cmd)

    generate_diffs = subparsers.add_parser("generate-diffs",
                                           help="generate diffs between two branches and output bot message")
    generate_diffs.add_argument("--base-sha", help="base commit to diff against (SHA-like)")
    generate_diffs.add_argument("base_branch", help="name of the base branch to diff against")
    generate_diffs.add_argument("head_branch", help="name of the head branch to diff against")
    generate_diffs.set_defaults(cmd=_generate_diffs_cmd)

    return parser


def main():
    cli = create_cli()
    opts = cli.parse_args()
    print(opts)

    if opts.head_sha is None:
        opts.head_sha = get_cmd_output("git rev-parse HEAD")

    print(f"using head sha: {opts.head_sha}")

    opts.cmd(opts)


if __name__ == '__main__':
    main()
