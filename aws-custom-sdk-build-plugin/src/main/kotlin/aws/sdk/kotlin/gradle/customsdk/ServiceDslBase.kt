package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultFileCollectionDependency
import org.gradle.api.provider.Provider

public sealed class ServiceDslBase(
    protected val extension: AwsCustomSdkBuildExtension,
    protected val serviceName: String,
) {
    private val operationNames = mutableSetOf<String>()

    protected fun addOperation(name: String) {
        if (!operationNames.add(name)) {
            extension.project.logger.warn("Operation $serviceName.$name added multiple times")
        }
    }

    internal fun toDependency(): Provider<Dependency> {
        extension.project.provider {
            DefaultFileCollectionDependency()
        }
    }
}
