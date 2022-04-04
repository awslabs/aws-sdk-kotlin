/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

 // Init script used by build and release automation

gradle.projectsLoaded {
    rootProject.allprojects {
        buildDir = rootProject.file("build-dir/${rootProject.name}/${project.name}")
    }
}
