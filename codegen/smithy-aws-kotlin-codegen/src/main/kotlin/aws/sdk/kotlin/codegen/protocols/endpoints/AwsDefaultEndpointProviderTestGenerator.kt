package aws.sdk.kotlin.codegen.protocols.endpoints

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.DefaultEndpointProviderTestGenerator
import software.amazon.smithy.rulesengine.language.EndpointRuleSet
import software.amazon.smithy.rulesengine.traits.EndpointTestCase

class AwsDefaultEndpointProviderTestGenerator(
    writer: KotlinWriter,
    rules: EndpointRuleSet,
    cases: List<EndpointTestCase>,
    providerSymbol: Symbol,
    paramsSymbol: Symbol,
): DefaultEndpointProviderTestGenerator(writer, rules, cases, providerSymbol, paramsSymbol) {
    override val expectedPropertyRenderers = mapOf(
        "authSchemes" to ::renderAuthSchemes,
    )
}