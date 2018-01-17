import org.gradle.api.publication.maven.internal.deployer.BaseMavenInstaller

val mavenEmbedder: Configuration = configurations.create("mavenEmbedder")
val testProjectDir = "${project.parent.projectDir}/test-project/"
val install = tasks.findByName("install") as Upload
val mavenInstaller = install.repositories.findByName("mavenInstaller") as BaseMavenInstaller

dependencies {
    compile("org.apache.maven:maven-plugin-api:3.5.0")
    compile("org.apache.maven.plugin-tools:maven-plugin-annotations:3.5")
    compile("org.apache.maven:maven-project:2.2.1")
    compile(project(":codegen"))

    mavenEmbedder("org.apache.maven:maven-embedder:3.5.0")
    mavenEmbedder("org.slf4j:slf4j-simple:1.7.5")
    mavenEmbedder("org.eclipse.aether:aether-connector-basic:1.0.2.v20150114")
    mavenEmbedder("org.eclipse.aether:aether-transport-wagon:1.0.2.v20150114")
    mavenEmbedder("org.apache.maven.wagon:wagon-http:2.9:shaded")
    mavenEmbedder("org.apache.maven.wagon:wagon-provider-api:2.9")
}

mavenInstaller.pom.apply {
    groupId = "${project.group}"
    artifactId = project.name
    version = "${project.version}"
    packaging = "maven-plugin"
}

val compileTestProject = task(name = "compileTestProject", type = JavaExec::class) {
    description = "Generate Kotlin SDK in test project using gradle plugin and compile."
    inputs.files("$testProjectDir/src", "$testProjectDir/pom.xml")
    outputs.file("${project.parent.projectDir}/test-project/build/generated-sources")
    dependsOn(":codegen:install", "install")
    tasks.findByName("test").dependsOn(this)

    systemProperty("maven.multiModuleProjectDirectory", "${project.parent.projectDir}/test-project")
    classpath(mavenEmbedder)
    main = "org.apache.maven.cli.MavenCli"
    args = listOf("test-compile", "--file", testProjectDir, "-B")
}

task("integrationTestProject", type = JavaExec::class) {
    description = "Runs integration tests on test project."
    inputs.file("${project.parent.projectDir}/test-project/build/generated-sources")
    dependsOn(compileTestProject)
    tasks.findByName("integrationTest").dependsOn(this)

    systemProperty("maven.multiModuleProjectDirectory", "${project.parent.projectDir}/test-project")
    classpath(mavenEmbedder)
    main = "org.apache.maven.cli.MavenCli"
    args = listOf("test", "--file", testProjectDir, "-B")
}

(tasks.findByName("clean") as Delete).delete("${project.parent.projectDir}/test-project/target")

val generatePluginDescriptor = task("generatePluginDescriptor", type = JavaExec::class) {
    val compileKotlin = tasks.findByName("compileKotlin")
    val kotlinDestinationDir = compileKotlin.property("destinationDir") as File
    val pomFile = file("$buildDir/pom.xml")
    val pluginDescriptor = File(kotlinDestinationDir, "META-INF/maven/plugin.xml")

    inputs.files(compileKotlin.outputs.files)
    outputs.file(pluginDescriptor)

    classpath(mavenEmbedder)

    println(classpath)
    println(inputs.files)

    main = "org.apache.maven.cli.MavenCli"
    systemProperty("maven.multiModuleProjectDirectory", projectDir)
    args = listOf("--errors",
            "--batch-mode",
            "--file", "$buildDir/pom.xml",
            "org.apache.maven.plugins:maven-plugin-plugin:3.5:descriptor")

    doFirst {

        mavenInstaller.pom.withXml {
            asNode().appendNode("build").apply {
                appendNode("directory", buildDir.canonicalPath)
                appendNode("outputDirectory", kotlinDestinationDir.canonicalPath)
            }
        }.writeTo(pomFile)

        assert(pomFile.isFile, { "${pomFile.canonicalPath}: was not generated" })
        logger.info("POM is generated in ${pomFile.canonicalPath}")
    }

    doLast {
        assert(pluginDescriptor.isFile, { "${pluginDescriptor.canonicalPath}: was not generated" })
        logger.info("Plugin descriptor is generated in ${pluginDescriptor.canonicalPath}")
    }

    dependsOn(tasks.findByName("compileKotlin"))
    tasks.findByName("jar").dependsOn(this)
}
