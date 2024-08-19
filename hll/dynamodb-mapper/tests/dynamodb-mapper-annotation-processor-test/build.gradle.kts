import aws.sdk.kotlin.gradle.kmp.NATIVE_ENABLED
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import com.google.devtools.ksp.gradle.KspTaskJvm
import com.google.devtools.ksp.gradle.KspTaskMetadata
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask


/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper"))
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
            }
        }
    }
}

ksp {
    // annotation-processor-test does not need the ops-codegen processor loaded
    excludeProcessor("aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.HighLevelOpsProcessorProvider")
}

if (project.NATIVE_ENABLED) {
    dependencies.kspCommonMainMetadata(project(":hll:dynamodb-mapper:dynamodb-mapper-codegen"))

    kotlin.sourceSets.commonMain {
        tasks.withType<KspTaskMetadata> {
            // Wire up the generated source to the commonMain source set
            kotlin.srcDir(destinationDirectory)
        }
    }
} else {
    // FIXME This is a dirty hack for JVM-only builds which KSP doesn't consider to be "multiplatform".
    // Copied from dynamodb-mapper build.gradle.kts

    // Start by invoking the JVM-only KSP configuration
    dependencies.kspJvm(project(":hll:dynamodb-mapper:dynamodb-mapper-codegen"))

    // Then we need to move the generated source from jvm to common
    val moveGenSrc by tasks.registering {
        // Can't move src until the src is generated
        dependsOn(tasks.named("kspKotlinJvm"))

        // Detecting these paths programmatically is complex; just hardcode them
        val srcDir = file("build/generated/ksp/jvm/jvmMain")
        val destDir = file("build/generated/ksp/common/commonMain")

        inputs.dir(srcDir)
        outputs.dirs(srcDir, destDir)

        doLast {
            if (destDir.exists()) {
                // Clean out the existing destination, otherwise move fails
                require(destDir.deleteRecursively()) { "Failed to delete $destDir before moving from $srcDir" }
            } else {
                // Create the destination directories, otherwise move fails
                require(destDir.mkdirs()) { "Failed to create path $destDir" }
            }

            Files.move(srcDir.toPath(), destDir.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    tasks.withType<KotlinCompilationTask<*>> {
        if (this !is KspTaskJvm) {
            // Ensure that any **non-KSP** compile tasks depend on the generated src move
            dependsOn(moveGenSrc)
        }
    }

    // Finally, wire up the generated source to the commonMain source set
    kotlin.sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/common/commonMain/kotlin")
    }
}
