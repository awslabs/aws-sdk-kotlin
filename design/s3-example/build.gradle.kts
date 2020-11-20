/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

plugins {
    kotlin("jvm")
    application
}

val smithyKotlinClientRtVersion: String by project
val smithyKotlinGroup: String = "software.aws.smithy.kotlin"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("$smithyKotlinGroup:client-rt-core:$smithyKotlinClientRtVersion")
    implementation("$smithyKotlinGroup:http:$smithyKotlinClientRtVersion")
    implementation("$smithyKotlinGroup:http-client-engine-ktor:$smithyKotlinClientRtVersion")
    implementation("$smithyKotlinGroup:serde:$smithyKotlinClientRtVersion")
    implementation("$smithyKotlinGroup:serde-xml:$smithyKotlinClientRtVersion")

    // FIXME - this is only necessary for a conversion from ByteStream to HttpBody (which belongs in client runtime)
    implementation("$smithyKotlinGroup:io:$smithyKotlinClientRtVersion")

    // FIXME - this isn't necessary it's only here for the example main function
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
}

//compileKotlin {
//    kotlinOptions.jvmTarget = "1.8"
//}
//
//compileTestKotlin {
//    kotlinOptions.jvmTarget = "1.8"
//}
// FIXME - intellij 2020.2 is not resolving mpp dependencies and the default run configurations are not adding
// project dependencies to the classpath. As a workaround just use gradle (i.e ./gradlew :example:s3-example:application)
// Similar to: https://youtrack.jetbrains.com/issue/KT-38651
application {
    mainClassName = "com.amazonaws.service.s3.DefaultS3ClientKt"
}
