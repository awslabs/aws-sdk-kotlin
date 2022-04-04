/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

 // Init script used by build and release automation

gradle.projectsLoaded {
    rootProject.allprojects {
        // out of source build - place all build artifacts in a single location
        buildDir = rootProject.file("build-dir/${rootProject.name}/${project.name}")
    }
}
