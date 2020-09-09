description = "HTTP core for AWS service clients"
extra["displayName"] = "Software :: AWS :: KotlinSdk :: HTTP"
extra["moduleName"] = "software.aws.kotlinsdk.http"

val smithyKotlinClientRtVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":client-runtime:aws-client-rt"))

                api("software.aws.smithy.kotlin:http:$smithyKotlinClientRtVersion")
                api("software.aws.smithy.kotlin:http-serde:$smithyKotlinClientRtVersion")
            }
        }
    }
}
