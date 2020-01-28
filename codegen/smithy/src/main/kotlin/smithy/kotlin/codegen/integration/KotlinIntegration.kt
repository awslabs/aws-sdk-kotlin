/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package smithy.kotlin.codegen.integration

import smithy.kotlin.codegen.KotlinSettings
import smithy.kotlin.codegen.KotlinWriter
import smithy.kotlin.codegen.LanguageTarget
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * Java SPI for customizing TypeScript code generation, registering
 * new protocol code generators, renaming shapes, modifying the model,
 * adding custom code, etc.
 */
interface KotlinIntegration {
    /**
     * Preprocess the model before code generation.
     *
     *
     * This can be used to remove unsupported features, remove traits
     * from shapes (e.g., make members optional), etc.
     *
     * @param context Plugin context.
     * @param settings Setting used to generate.
     * @return Returns the updated model.
     */
    fun preprocessModel(context: PluginContext, settings: KotlinSettings?): Model? {
        return context.model
    }

    /**
     * Updates the [SymbolProvider] used when generating code.
     *
     *
     * This can be used to customize the names of shapes, the package
     * that code is generated into, add dependencies, add imports, etc.
     *
     * @param settings Setting used to generate.
     * @param model Model being generated.
     * @param symbolProvider The original `SymbolProvider`.
     * @return The decorated `SymbolProvider`.
     */
    fun decorateSymbolProvider(settings: KotlinSettings, model: Model, symbolProvider: SymbolProvider): SymbolProvider {
        return symbolProvider
    }

    /**
     * Called each time a writer is used that defines a shape.
     *
     * This method could be called multiple times for the same writer
     * but for different shapes. It gives an opportunity to intercept code
     * sections of a [KotlinWriter] by name using the shape for
     * context. For example:
     *
     * ```
     * public final class MyIntegration implements TypeScriptIntegration {
     *     public onWriterUse(KotlinSettings settings, Model model, SymbolProvider symbolProvider,
     *             KotlinWriter writer, Shape definedShape) {
     *         writer.onSection("example", text -&gt; writer.write("Intercepted: " + text"));
     *     }
     * }
     * ```
     *
     * Any mutations made on the writer (for example, adding
     * section interceptors) are removed after the callback has completed;
     * the callback is invoked in between pushing and popping state from
     * the writer.
     *
     * @param settings Settings used to generate.
     * @param model Model to generate from.
     * @param symbolProvider Symbol provider used for codegen.
     * @param writer Writer that will be used.
     * @param definedShape Shape that is being defined in the writer.
     */
    fun onShapeWriterUse(
        settings: KotlinSettings?,
        model: Model?,
        symbolProvider: SymbolProvider?,
        writer: KotlinWriter?,
        definedShape: Shape?
    ) {
        // pass
    }

    /**
     * Writes additional files.
     *
     * ```
     * public final class MyIntegration implements KotlinIntegration {
     *     public writeAdditionalFiles(
     *             KotlinSettings settings,
     *             Model model,
     *             SymbolProvider symbolProvider,
     *             BiConsumer<String, Consumer<KotlinWriter>> writerFactory
     *     ) {
     *         writerFactory.accept("foo.ts", writer -> {
     *             writer.write("// Hello!");
     *         });
     *     }
     * }
     * ```
     *
     * @param settings Settings used to generate.
     * @param model Model to generate from.
     * @param symbolProvider Symbol provider used for codegen.
     * @param writerFactory A factory function that takes the name of a file
     * to write and a `Consumer` that receives a
     * [KotlinWriter] to perform the actual writing to the file.
     */
    fun writeAdditionalFiles(
        settings: KotlinSettings?,
        model: Model?,
        symbolProvider: SymbolProvider?,
        writerFactory: BiConsumer<String, Consumer<KotlinWriter>?>?
    ) {
        // pass
    }

    /**
     * Gets a list of plugins to apply to the generated client.
     *
     * @return Returns the list of RuntimePlugins to apply to the client.
     */
    val clientPlugins: List<Any?>?
        get() = emptyList()

    /**
     * Gets a list of protocol generators to register.
     *
     * @return Returns the list of protocol generators to register.
     */
    val protocolGenerators: List<ProtocolGenerator>
        get() = emptyList()

    /**
     * Adds additional client config interface fields.
     *
     *
     * Implementations of this method are expected to add fields to the
     * "ClientDefaults" interface of a generated client. This interface
     * contains fields that are either statically generated from
     * a model or are dependent on the runtime that a client is running in.
     * Implementations are expected to write interface field names and
     * their type signatures, each followed by a semicolon (;). Any number
     * of fields can be added, and any [Symbol] or
     * [SymbolReference] objects that are written to the writer are
     * automatically imported, and any of their contained
     * [SymbolDependency] values are automatically added to the
     * generated `package.json` file.
     *
     *
     * For example, the following code adds two fields to a client:
     *
     * ```
     * `public final class MyIntegration implements TypeScriptIntegration {
     * public void addConfigInterfaceFields(
     * KotlinSettings settings,
     * Model model,
     * SymbolProvider symbolProvider,
     * KotlinWriter writer
     * ) {
     * writer.writeDocs("The docs for foo...");
     * writer.write("foo?: string;"); // Note the trailing semicolon!
     *
     * writer.writeDocs("The docs for bar...");
     * writer.write("bar?: string;");
     * }
     * }
    `</pre> *
     *
     * @param settings Settings used to generate.
     * @param model Model to generate from.
     * @param symbolProvider Symbol provider used for codegen.
     * @param writer TypeScript writer to write to.
     */
    fun addConfigInterfaceFields(
        settings: KotlinSettings?,
        model: Model?,
        symbolProvider: SymbolProvider?,
        writer: KotlinWriter?
    ) {
        // pass
    }

    /**
     * Adds additional runtime-specific or shared client config values.
     *
     * Implementations of this method are expected to add values to
     * a runtime-specific or shared configuration object that is used to
     * provide values for a "ClientDefaults" interface. This method is
     * invoked for every supported [LanguageTarget]. Implementations are
     * expected to branch on the provided `LanguageTarget` and add
     * the appropriate default values and imports, each followed by a
     * (,). Any number of key-value pairs can be added, and any [Symbol]
     * or [SymbolReference] objects that are written to the writer are
     * automatically imported, and any of their contained
     * [SymbolDependency] values are automatically added to the
     * generated `package.json` file.
     *
     * For example, the following code adds two values for both the
     * node and browser targets and ignores the SHARED target:
     *
     * ```
     * `public final class MyIntegration implements TypeScriptIntegration {
     *
     *     public void addRuntimeConfigValues(
     *             TypeScriptSettings settings,
     *             Model model,
     *             SymbolProvider symbolProvider,
     *             TypeScriptWriter writer,
     *             LanguageTarget target
     *     ) {
     *         // This is a static value that is added to every generated
     *         // runtimeConfig file.
     *         writer.write("foo: 'some-static-value',"); // Note the trailing comma!
     *
     *         switch (target) {
     *             case NODE:
     *                 writer.write("bar: someNodeValue,");
     *                 break;
     *             case BROWSER:
     *                 writer.write("bar: someBrowserValue,");
     *                 break;
     *             case SHARED:
     *                 break;
     *             default:
     *                 LOGGER.warn("Unknown target: " + target);
     *         }
     *     }
     * }
     * ```
     *
     * The following code adds a value to the runtimeConfig.shared.ts file
     * so that it used on all platforms. It pulls a trait value from the
     * service being generated and adds it to the client configuration. Note
     * that a corresponding entry needs to be added to
     * [.addConfigInterfaceFields] to make TypeScript aware of the
     * property.
     *
     * ```
     * public final class MyIntegration2 implements TypeScriptIntegration {
     *     public void addRuntimeConfigValues(
     *             TypeScriptSettings settings,
     *             Model model,
     *             SymbolProvider symbolProvider,
     *             TypeScriptWriter writer,
     *             LanguageTarget target
     *     ) {
     *         if (target == LanguageTarget.SHARED) {
     *             String someTraitValue = settings.getModel(model).getTrait(SomeTrait.class)
     *                          .map(SomeTrait::getValue)
     *                          .orElse("");
     *             writer.write("someTraitValue: $S,", someTraitValue);
     *         }
     *     }
     * }
     * ```
     *
     * @param settings Settings used to generate.
     * @param model Model to generate from.
     * @param symbolProvider Symbol provider used for codegen.
     * @param writer TypeScript writer to write to.
     * @param target The TypeScript language target.
     */
    fun addRuntimeConfigValues(
        settings: KotlinSettings?,
        model: Model?,
        symbolProvider: SymbolProvider?,
        writer: KotlinWriter?,
        target: LanguageTarget?
    ) {
        // pass
    }
}