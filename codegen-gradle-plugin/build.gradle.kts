val compileTestProject = task(name = "compileTestProject", type = GradleBuild::class) {
    description = "Generate Kotlin SDK in test project using gradle plugin and compile."
    buildFile = file("${project.parent.projectDir}/test-project/build.gradle")
    tasks = listOf("compileTestKotlin")
    outputs.file("${project.parent.projectDir}/test-project/build/generated-src")
    dependsOn(":codegen:install", "install")
}

tasks.findByName("test").dependsOn(compileTestProject)

val integrationTestProject = task(name = "integrationTestProject",
        type = GradleBuild::class) {
    description = "Runs integration tests on test project."
    buildFile = file("${project.parent.projectDir}/test-project/build.gradle")
    tasks = listOf("test")
    inputs.file("${project.parent.projectDir}/test-project/build/generated-src")
    dependsOn(compileTestProject)
}

tasks.findByName("integrationTest").dependsOn(integrationTestProject)

(tasks.findByName("clean") as Delete).delete("${project.parent.projectDir}/test-project/build")

dependencies {
    compile(project(":codegen"))
    compile(gradleApi())
}
