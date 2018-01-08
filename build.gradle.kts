import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "software.amazon.awssdk.kotlin"
version = "1.0-SNAPSHOT"

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.0"

    repositories {
        mavenCentral()
    }
    
    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlin_version))
    }
    
}

apply {
    plugin("java")
    plugin("kotlin")
}

val kotlin_version: String by extra

repositories {
    mavenCentral()
}

dependencies {
    compile("software.amazon.awssdk", "codegen", "2.0.0-preview-7")
    compile("com.squareup:kotlinpoet:0.6.0")

    compile("software.amazon.awssdk", "sts", "2.0.0-preview-7") //remove this when done testing

    compile(kotlinModule("stdlib-jdk8", kotlin_version))
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

