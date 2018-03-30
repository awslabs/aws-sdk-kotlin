val compileTestProject = task(name = "compileTestProject", type = GradleBuild::class) {
    description = "Generate Kotlin SDK in test project using gradle plugin and compile."
    outputs.file("${project.parent?.projectDir}/test-project/build/generated-src")
    dependsOn(":codegen:install", "install")
    project.tasks?.findByName("test")?.dependsOn(this)

    buildFile = file("${project.parent?.projectDir}/test-project/build.gradle")
    tasks = listOf("compileTestKotlin")
}

val integrationTestProject = task(name = "integrationTestProject",
        type = GradleBuild::class) {
    description = "Runs integration tests on test project."
    inputs.file("${project.parent?.projectDir}/test-project/build/generated-src")
    dependsOn(compileTestProject)
    project.tasks?.findByName("integrationTest")?.dependsOn(this)

    buildFile = file("${project.parent?.projectDir}/test-project/build.gradle")
    tasks = listOf("test")
}

(tasks?.findByName("clean") as Delete).delete("${project.parent?.projectDir}/test-project/build")

dependencies {
    compile(project(":codegen"))
    compile(gradleApi())
}
