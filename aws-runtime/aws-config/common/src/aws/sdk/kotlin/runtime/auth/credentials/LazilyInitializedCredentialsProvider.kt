package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.businessmetrics.mergeBusinessMetrics
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.MutableAttributes
import aws.smithy.kotlin.runtime.collections.mergeExcept
import aws.smithy.kotlin.runtime.operation.ExecutionContext

/**
 * A [CredentialsProvider] implementation that delays the initialization of the underlying provider until
 * the first call to [resolve]. This is useful when the initialization of the credentials provider is expensive
 * or should be deferred until credentials are actually needed.
 *
 * @param providerName The name of the credentials provider that is being wrapped. Will default to "LazilyInitializedCredentialsProvider".
 * @param executionContext Additional execution context to use when resolving credentials. Will default to an empty execution context.
 * @param initializer A lambda function that provides the actual [CredentialsProvider] to be initialized lazily.
 */
public class LazilyInitializedCredentialsProvider(
    private val providerName: String = "LazilyInitializedCredentialsProvider",
    private val executionContext: ExecutionContext = ExecutionContext(),
    initializer: () -> CredentialsProvider,
) : CredentialsProvider {
    private val provider = lazy(initializer)

    override suspend fun resolve(attributes: Attributes): Credentials {
        if (attributes is MutableAttributes) {
            attributes.mergeExcept(executionContext, exceptions = setOf(BusinessMetrics))
            attributes.mergeBusinessMetrics(executionContext)
        }
        return provider.value.resolve(attributes)
    }

    override fun toString(): String = providerName
}
