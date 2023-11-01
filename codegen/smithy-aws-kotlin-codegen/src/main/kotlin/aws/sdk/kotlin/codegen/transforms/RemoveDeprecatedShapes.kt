package aws.sdk.kotlin.codegen.transforms

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.build.transforms.ConfigurableProjectionTransformer
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.DeprecatedTrait
import java.time.Instant
import java.util.function.Predicate

class RemoveDeprecatedShapes : ConfigurableProjectionTransformer<RemoveDeprecatedShapes.Config>() {
    class Config {
        var since: String = "1970-01-01" // YYYY-MM-DD
    }

    override fun getName(): String = "AwsSdkKotlinRemoveDeprecatedShapes"

    override fun getConfigType(): Class<Config> = Config::class.java

    /**
     * Filter out shapes which have a [DeprecatedTrait] with a `since` property set to before the configured [Config.since]
     */
    override fun transformWithConfig(context: TransformContext, config: Config): Model {
        val deprecated = Predicate<Shape> { shape ->
            val deprecatedTrait = shape.getTrait<DeprecatedTrait>()

            val modeledSince = deprecatedTrait?.since?.getOrNull()?.let {
                Instant.parse(it)
            }

            (deprecatedTrait != null && modeledSince != null && modeledSince < Instant.parse(config.since))
        }

        return context.transformer.removeShapesIf(context.model, deprecated)
//        return context.transformer.filterShapes(context.model) { shape ->
//            if (!shape.hasTrait<DeprecatedTrait>()) { return@filterShapes false }
//
//            val deprecatedSince = shape
//                .getTrait<DeprecatedTrait>()!!
//                .since.getOrNull()
//                ?.let {
//                    println("Parsing date string $it")
//                    Instant.parse(it)
//                }
//                ?: return@filterShapes false
//
//            deprecatedSince < Instant.parse(config.since)
//        }
    }
}