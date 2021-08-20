plugins {
    kotlin("jvm")
    id("software.amazon.smithy").version("0.5.3")
}

val awsSdkKotlinVersion: String by project

dependencies {
    implementation("software.amazon.smithy.kotlin:smithy-aws-kotlin-codegen:$awsSdkKotlinVersion")
    // NOTE: More smithy dependencies may be required depending on what's referenced by your API models.
}
