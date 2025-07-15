package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.RuntimeTypePackage

object AwsPluginTypes {
    object CustomSdkBuild : RuntimeTypePackage(AwsKotlinDependency.AWS_CUSTOM_SDK_BUILD_PLUGIN) {
        val AwsCustomSdkBuildExtension = symbol("AwsCustomSdkBuildExtension")
        val ServiceDslBase = symbol("ServiceDslBase")
    }
}