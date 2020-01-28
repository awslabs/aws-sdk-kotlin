import software.amazon.smithy.gradle.tasks.SmithyBuild

plugins {
    id("software.amazon.smithy-fork")
}

dependencies {
    smithy(project(":smithy"))
}

// TODO Issues with this plugin
// 1. Auto adds in Java plugin, why? Seems a verification task but it doesnt run in Jar doenst run which is true for Java and TS...
// 2. The way it finds the classpath assumes it will be built first so it fails if its not ran
tasks.create<SmithyBuild>("buildSdk") {
    classpath = configurations.smithy.get()
}