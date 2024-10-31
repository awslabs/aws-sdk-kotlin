package aws.sdk.kotlin.runtime.util

import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.emitBusinessMetrics
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetrics
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.mutableAttributes
import aws.smithy.kotlin.runtime.collections.toMutableAttributes

/**
 * [Attributes] and any [BusinessMetric] that should be included.
 */
internal fun testAttributes(attributes: Attributes? = null, vararg metrics: BusinessMetric): Attributes {
    val testAttributes = attributes?.toMutableAttributes() ?: mutableAttributes()
    testAttributes.emitBusinessMetrics(metrics.toSet())
    return testAttributes
}

/**
 * [Attributes] that only contain the specified [BusinessMetric].
 */
internal fun testAttributes(vararg metrics: BusinessMetric): Attributes {
    val testAttributes = mutableAttributes()
    testAttributes.emitBusinessMetrics(metrics.toSet())
    return testAttributes
}

/**
 * Type alias for [emitBusinessMetrics], used for testing.
 */
internal fun Credentials.withBusinessMetrics(vararg metrics: BusinessMetric): Credentials =
    emitBusinessMetrics(metrics.toSet())
