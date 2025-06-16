kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":aws-runtime:aws-http"))
                implementation(libs.smithy.kotlin.http.client.engine.crt)
                api(project(":services:s3"))
            }
        }
    }
}