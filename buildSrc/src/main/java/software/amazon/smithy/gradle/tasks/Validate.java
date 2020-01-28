/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.gradle.tasks;

import org.gradle.api.tasks.TaskAction;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates the Smithy models.
 *
 * <p>The validation task will execute the Smithy CLI in a new process
 * to ensure that it uses an explicit classpath that ensures that the
 * generated JAR works correctly when used alongside its dependencies.
 *
 * <p>The CLI version used to validate the generated JAR is picked by
 * searching for smithy-model in the runtime dependencies. If found,
 * the same version of the CLI is used. If not found, a default version
 * is used.
 */
public class Validate extends SmithyCliTask {

    @TaskAction
    public void execute() {
        writeHeading("Running smithy validate");
        executeCliProcess("validate", ListUtils.of(), getClasspath(), getModelDiscoveryClasspath());
    }
}
