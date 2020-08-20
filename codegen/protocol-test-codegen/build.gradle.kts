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
import software.amazon.smithy.gradle.tasks.SmithyBuild

plugins {
    id("software.amazon.smithy") version "0.5.0"
}

val smithyVersion: String by project

dependencies {
    implementation("software.amazon.smithy:smithy-aws-protocol-tests:$smithyVersion")
    compile(project(":smithy-aws-kotlin-codegen"))
}

// This project doesn't produce a JAR.
tasks["jar"].enabled = false

// Run the SmithyBuild task manually since this project needs the built JAR
// from smithy-aws-kotlin-codegen.
tasks["smithyBuildJar"].enabled = false

tasks.create<SmithyBuild>("buildSdk") {
    addRuntimeClasspath = true
}

// Run the `buildSdk` automatically.
tasks["build"].finalizedBy(tasks["buildSdk"])

// force rebuild every time while developing
tasks["buildSdk"].outputs.upToDateWhen { false }

//// ensure built artifacts are put into the SDK's folders
//tasks.create<Exec>("copyGoCodegen") {
//    dependsOn ("buildSdk")
//    commandLine ("$rootDir/copy_go_codegen.sh", "$rootDir/../", "$buildDir", "github.com/aws/aws-sdk-go-v2/")
//}
//tasks["buildSdk"].finalizedBy(tasks["copyGoCodegen"])
