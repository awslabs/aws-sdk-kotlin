gradle.projectsLoaded {
    rootProject.allprojects {
        buildDir = rootProject.file("build-dir"/${rootProject.name}/${project.name}")
    }
}
