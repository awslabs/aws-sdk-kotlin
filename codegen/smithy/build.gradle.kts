plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("com.squareup:kotlinpoet:1.5.0")
    api("com.soywiz.korlibs.klock:klock-jvm:1.8.6")

    api("software.amazon.smithy:smithy-codegen-core:0.9.7")

    // TODO: Move this to aws smithy extension
    api("software.amazon.smithy:smithy-aws-traits:0.9.7")

    testApi(kotlin("test"))
    testApi(kotlin("test-junit"))
}