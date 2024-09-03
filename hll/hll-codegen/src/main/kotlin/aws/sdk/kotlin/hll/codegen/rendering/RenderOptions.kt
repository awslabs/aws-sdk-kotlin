package aws.sdk.kotlin.hll.codegen.rendering

import aws.sdk.kotlin.hll.codegen.rendering.RenderOptions.VisibilityAttribute
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.get

public object RenderOptions {

    /**
     * Determines the visibility of code-generated classes / objects. Defaults to [Visibility.DEFAULT].
     */
    public val VisibilityAttribute: AttributeKey<Visibility> = AttributeKey("Visibility")
}

/**
 * Determines the visibility of code-generated classes / objects. Defaults to [DEFAULT].
 */
public enum class Visibility {
    /**
     * A default, unspecified visibility will be used, which is recommended for most use-cases.
     */
    DEFAULT,

    /**
     * All code-generated constructs will be `public`
     */
    PUBLIC,

    /**
     * All code-generated constructs will be `internal`
     */
    INTERNAL,
}
