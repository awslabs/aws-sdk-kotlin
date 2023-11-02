/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.transforms

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.build.transforms.ConfigurableProjectionTransformer
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.DeprecatedTrait
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.function.Predicate

class RemoveDeprecatedShapes : ConfigurableProjectionTransformer<RemoveDeprecatedShapes.Config>() {
    class Config {
        var until: String = "1970-01-01" // yyyy-MM-dd
    }

    override fun getName(): String = "AwsSdkKotlinRemoveDeprecatedShapes"

    override fun getConfigType(): Class<Config> = Config::class.java

    /**
     * Filter out shapes which have a [DeprecatedTrait] with a `since` property _date_ set, up to [Config.until].
     * NOTE: Smithy supports modeling `since` as a version _or_ date, this transformer only considers those modeled as a date.
     */
    override fun transformWithConfig(context: TransformContext, config: Config): Model {
        val until = try {
            config.until.toLocalDate().also {
                println("Removing deprecated shapes using the configured `until` date $it")
            }
        } catch (e: DateTimeException) {
            throw IllegalArgumentException("Failed to parse configured `until` date ${config.until}", e)
        }

        return context.transformer.removeShapesIf(context.model, shouldRemoveDeprecatedShape(until))
    }
}

internal fun shouldRemoveDeprecatedShape(removeDeprecatedShapesUntil: LocalDate) = Predicate<Shape> { shape ->
    val since = shape.getTrait<DeprecatedTrait>()?.since?.getOrNull() ?: return@Predicate false

    val deprecatedDate = try { since.toLocalDate() } catch (e: DateTimeException) {
        println("Failed to parse `since` field $since as a date, skipping removal of deprecated shape $shape")
        return@Predicate false
    }

    val shouldRemove = deprecatedDate < removeDeprecatedShapesUntil

    shouldRemove.also {
        if (it) {
            println("Removing deprecated shape $shape since its modeled `since` field $deprecatedDate comes before the configured $removeDeprecatedShapesUntil date.")
        }
    }
}

/**
 * Parses a string of yyyy-MM-dd format to [LocalDate].
 */
internal fun String.toLocalDate(): LocalDate {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return LocalDate.parse(this, formatter)
}
