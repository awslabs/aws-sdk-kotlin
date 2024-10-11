package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes

/**
 * A [CredentialsProvider] implementation that delays the initialization of the underlying provider until
 * the first call to [resolve]. This is useful when the initialization of the credentials provider is expensive
 * or should be deferred until credentials are actually needed.
 *
 * @param providerName The name of the credentials provider that is being wrapped.
 * @param initializer A lambda function that provides the actual [CredentialsProvider] to be initialized lazily.
 */
public class LazilyInitializedCredentialsProvider(
    override val providerName: String? = null,
    initializer: () -> CredentialsProvider,
) : CredentialsProvider {
    private val provider = lazy(initializer)

    override suspend fun resolve(attributes: Attributes): Credentials = provider.value.resolve(attributes)
}
