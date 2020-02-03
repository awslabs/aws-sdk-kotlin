import software.amazon.smithy.gradle.tasks.SmithyBuild
import software.amazon.smithy.model.node.Node

plugins {
    id("software.amazon.smithy-fork")
}

dependencies {
    smithyCli(project(":smithy"))
    // TODO: The plugin is not adding CLI correctly...add here as hack
    smithyCli("software.amazon.smithy:smithy-cli:0.9.7")
}

tasks.register("generate-smithy-build") {
    doLast {
        val projectionsBuilder = Node.objectNodeBuilder()

        fileTree("aws-models").filter { it.isFile }.files.forEach { file ->
            val (sdkId, version, remaining) = file.name.split(".")
            val projectionContents = Node.objectNodeBuilder()
                .build()
            projectionsBuilder.withMember(sdkId + "." + version.toLowerCase(), projectionContents)
        }

        file("smithy-build.json").writeText(Node.prettyPrintJson(Node.objectNodeBuilder()
            .withMember("version", "1.0")
            .withMember("projections", projectionsBuilder.build())
            .build()))
    }
}

// TODO Issues with this plugin
// 1. Auto adds in Java plugin, why? Seems a verification task but it doesnt run in Jar doenst run which is true for Java and TS...
// 2. The way it finds the classpath assumes it will be built first so it fails if its not ran
tasks.create<SmithyBuild>("buildSdk") {
    classpath = configurations.smithyCli.get()
    models = files(project.file("model/workmailmessageflow.2019-05-01.json"))
}