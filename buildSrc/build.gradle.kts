plugins {
    `kotlin-dsl`
    `java-library`
}

repositories {
    mavenCentral()
    jcenter()
}

gradlePlugin {
    plugins {
        register("smithy-plugin-fork") {
            id = "software.amazon.smithy-fork"
            implementationClass = "software.amazon.smithy.gradle.SmithyPlugin"
        }
    }
}

dependencies {
    implementation("software.amazon.smithy:smithy-model:0.9.7")
    implementation("software.amazon.smithy:smithy-build:0.9.7")
    implementation("software.amazon.smithy:smithy-cli:0.9.7")
}