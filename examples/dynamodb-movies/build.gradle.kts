plugins {
    kotlin("jvm")
}

val awsSdkKotlinVersion: String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("aws.sdk.kotlin:dynamodb:$awsSdkKotlinVersion")
    implementation("com.google.code.gson:gson:2.8.6")
}
