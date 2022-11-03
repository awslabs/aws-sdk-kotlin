#!/usr/bin/env python3
#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
#
# This script can be run and tested locally. To do so, you should check out
# a second aws-sdk-kotlin repository so that you can work on the script and still
# run it without it immediately bailing for an unclean working tree (and to avoid creating
# temporary branches).
#
# Example:
# `aws-sdk-kotlin/` - the main repo you're working out of
# `/tmp/aws-sdk-kotlin/` - the repo you're testing against
#
# ```
# $ cd test/aws-sdk-kotlin
# $ ../../aws-sdk-kotlin/.github/scripts/codegen-diff-revisions.py . <some commit hash to diff against>
# ```
#
# It will diff the generated code from HEAD against any commit hash you feed it. If you want to test
# a specific range, change the HEAD of the test repository.
#
# This script requires `diff2html-cli` to be installed from NPM:
# ```
# $ npm install -g diff2html-cli@5.2.5
# ```
# Make sure the local version matches the version referenced from the GitHub Actions workflow.

import argparse
import os
import sys
import subprocess
import tempfile
import shlex

HEAD_BRANCH_NAME = "__tmp-localonly-head"
BASE_BRANCH_NAME = "__tmp-localonly-base"
OUTPUT_PATH = "tmp-codegen-diff/"

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


# Writes an HTML template for diff2html so that we can add contextual information
def write_html_template(title, subtitle, tmp_file):
    tmp_file.writelines(map(lambda line: line.encode(), [
        "<!doctype html>",
        "<html>",
        "<head>",
        '  <metadata charset="utf-8">',
        f'  <title>Codegen diff for the {title}: {subtitle}</title>',
        '  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.6.0/styles/github.min.css" / >',
        '  <!--diff2html-css-->',
        '  <!--diff2html-js-ui-->',
        '  <script>',
        '  document.addEventListener("DOMContentLoaded", () => {',
        '    const targetElement = document.getElementById("diff");',
        '    const diff2htmlUi = new Diff2HtmlUI(targetElement);',
        '    //diff2html-fileListToggle',
        '    //diff2html-synchronisedScroll',
        '    //diff2html-highlightCode',
        '  });',
        '  </script>',
        "</head>",
        '<body style="font-family: sans-serif;">',
        f"  <h1>Codegen diff for the {title}</h1>",
        f"  <p>{subtitle}</p>",
        '  <div id="diff">',
        '    <!--diff2html-diff-->',
        '  </div>',
        "</body>",
        "</html>",
    ]))
    tmp_file.flush()


def make_diff(title, path_to_diff, base_sha, head_sha, suffix, ignore_whitespace):
    ws_flag = "-b" if ignore_whitespace else ""
    diff_exists = get_cmd_status(f"git diff --quiet {ws_flag} {BASE_BRANCH_NAME} {HEAD_BRANCH_NAME} -- {path_to_diff}")

    if diff_exists == 0:
        eprint(f"No diff output for {base_sha}..{head_sha}")
        return None

    run(f'mkdir -p {OUTPUT_PATH}/{base_sha}/{head_sha}')
    dest_path = f"{base_sha}/{head_sha}/diff-{suffix}.html"
    whitespace_context = "(ignoring whitespace)" if ignore_whitespace else ""
    with tempfile.NamedTemporaryFile() as tmp_file:
        write_html_template(title, f"rev. {head_sha} {whitespace_context}", tmp_file)

        # Generate HTML diff. This uses the diff2html-cli, which defers to `git diff` under the hood.
        # All arguments after the first `--` go to the `git diff` command.
        diff_cmd = f"diff2html -s line -f html -d word -i command --hwt " \
                   f"{tmp_file.name} -F {OUTPUT_PATH}/{dest_path} -- " \
                   f"-U20 {ws_flag} {BASE_BRANCH_NAME} {HEAD_BRANCH_NAME} -- {path_to_diff}"
        eprint(f"Running diff cmd: {diff_cmd}")
        run(diff_cmd)
    return dest_path


def diff_link(diff_text, empty_diff_text, diff_location, alternate_text, alternate_location):
    if diff_location is None:
        return empty_diff_text

    return f"[{diff_text}]({CDN_URL}/codegen-diff/{diff_location}) ([{alternate_text}]({CDN_URL}/codegen-diff/{alternate_location}))"


def make_diffs(base_sha, head_sha):
    sdk_ws = make_diff('AWS SDK', f'{OUTPUT_PATH}/services', base_sha, head_sha, 'aws-sdk', ignore_whitespace=False)
    sdk_no_ws = make_diff('AWS SDK', f'{OUTPUT_PATH}/services', base_sha, head_sha, 'aws-sdk-ignore-ws',
                          ignore_whitespace=True)

    sdk_links = diff_link('AWS SDK', 'No codegen difference in the AWS SDK', sdk_ws, 'ignoring whitespace', sdk_no_ws)

    return f'A new generated diff is ready to view.\\n\\n- {sdk_links}\\n'


def create_cli():
    parser = argparse.ArgumentParser(
        prog="codegen-diff-revisions",
        description="Generate HTML diffs of codegen output",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )

    parser.add_argument("repo_root", help="repository root")
    parser.add_argument("base_sha", help="base commit to diff against (SHA-like)")
    parser.add_argument("--bootstrap", help="services to pass to bootstrap and include in diff output",
                        default="+dynamodb,+codebuild,+sts,+ec2,+polly,+s3")
    parser.add_argument("--head-sha", help="head commit to use (defaults to whatever current HEAD) is")

    return parser


def main():
    cli = create_cli()
    opts = cli.parse_args()
    print(opts)

    os.chdir(opts.repo_root)

    if opts.head_sha is None:
        head_sha = get_cmd_output("git rev-parse HEAD")
    else:
        head_sha = opts.head_sha

    print(f"using head sha is {head_sha}")

    # Make sure the working tree is clean
    if get_cmd_status("git diff --quiet") != 0:
        eprint("working tree is not clean. aborting")
        sys.exit(1)

    # Generate code for HEAD
    print(f"Creating temporary branch with generated code for the HEAD revision {head_sha}")
    run(f"git checkout {head_sha} -b {HEAD_BRANCH_NAME}")
    generate_and_commit_generated_code(head_sha, opts.bootstrap)

    # Generate code for base
    print(f"Creating temporary branch with generated code for the base revision {opts.base_sha}")
    run(f"git checkout {opts.base_sha} -b {BASE_BRANCH_NAME}")
    generate_and_commit_generated_code(opts.base_sha, opts.bootstrap)

    bot_message = make_diffs(opts.base_sha, head_sha)
    with open(f"{OUTPUT_PATH}/bot-message", 'w') as f:
        f.write(bot_message)

    # cleanup
    if not running_in_github_action():
        run(f"git checkout main")
        run(f"git branch -D {BASE_BRANCH_NAME}")
        run(f"git branch -D {HEAD_BRANCH_NAME}")


if __name__ == '__main__':
    main()
