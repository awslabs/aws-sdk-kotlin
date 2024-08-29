package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaGeneratorPluginTest {
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: File

    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var runner: GradleRunner

    private val sdkVersion = getSdkVersion()

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts")
        buildFile = File(testProjectDir, "build.gradle.kts")

        // Apply the plugin
        val buildFileContent = """
        repositories {
            mavenCentral()
            mavenLocal()
        }
        
        plugins {
            id("org.jetbrains.kotlin.jvm") version "2.0.10"
            id("aws.sdk.kotlin.hll.dynamodbmapper.schema.generator")
        }
        
        dependencies {
            implementation("aws.sdk.kotlin:dynamodb-mapper:$sdkVersion")
            implementation("aws.sdk.kotlin:dynamodb-mapper-annotations:$sdkVersion")
            implementation("aws.sdk.kotlin:dynamodb-mapper-codegen:$sdkVersion")
            implementation("aws.sdk.kotlin:dynamodb-mapper-schema-generator-plugin:$sdkVersion")
        }
        
        """.trimIndent()
        buildFile.writeText(buildFileContent)

        runner = GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withGradleVersion("8.5") // TODO parameterize
            .forwardOutput()
    }

    @AfterEach
    fun cleanup() {
//        if (settingsFile.exists()) {
//            settingsFile.delete()
//        }
//        if (buildFile.exists()) {
//            buildFile.delete()
//        }
    }

    private fun File.prependText(text: String) {
        val existingContent = readText()
        writeText(text)
        appendText(existingContent)
    }

    private fun getResource(resourceName: String): String = this::class.java
        .getResourceAsStream(resourceName)
        ?.bufferedReader().use {
            it?.readText()
        } ?: error("Could not read $resourceName")

    private fun createClassFile(className: String) {
        val classFile = File(testProjectDir, "src/main/kotlin/org/example/$className.kt")
        classFile.ensureParentDirsCreated()
        classFile.createNewFile()
        classFile.writeText(getResource("/$className.kt"))
    }

    @Test
    fun testConfiguringPlugin() {
        val buildFileContent = """
        import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
        dynamoDbMapper {
            generateBuilderClasses = GenerateBuilderClasses.WHEN_REQUIRED
        }
        
        """.trimIndent()
        buildFile.prependText(buildFileContent)

        val result = runner
            .withArguments("--info", "build")
            .build()

        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)
    }

    @Test
    fun testDefaultOptions() {
        createClassFile("User")

        val result = runner
            .withArguments("--info", "build")
            .build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/mapper/schemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Builder
        assertContains(schemaContents, """
        /**
         * A DSL-style builder for instances of [User]
         */
        public class UserBuilder {
            public var id: Int? = null
            public var givenName: String? = null
            public var surname: String? = null
            public var age: Int? = null

            public fun build(): User {
                val id = requireNotNull(id) { "Missing value for id" }
                val givenName = requireNotNull(givenName) { "Missing value for givenName" }
                val surname = requireNotNull(surname) { "Missing value for surname" }
                val age = requireNotNull(age) { "Missing value for age" }

                return User(
                    id,
                    givenName,
                    surname,
                    age,
                )
            }
        }
        """.trimIndent())

        // Converter
        assertContains(schemaContents, """
        public object UserConverter : ItemConverter<User> by SimpleItemConverter(
            builderFactory = ::UserBuilder,
            build = UserBuilder::build,
            descriptors = arrayOf(
                AttributeDescriptor(
                    "id",
                    User::id,
                    UserBuilder::id::set,
                    IntConverter
                ),
                AttributeDescriptor(
                    "fName",
                    User::givenName,
                    UserBuilder::givenName::set,
                    StringConverter
                ),
                AttributeDescriptor(
                    "lName",
                    User::surname,
                    UserBuilder::surname::set,
                    StringConverter
                ),
                AttributeDescriptor(
                    "age",
                    User::age,
                    UserBuilder::age::set,
                    IntConverter
                ),
            ),
        )
        """.trimIndent())

        // Schema
        assertContains(schemaContents, """
        public object UserSchema : ItemSchema.PartitionKey<User, Int> {
            override val converter : UserConverter = UserConverter
            override val partitionKey: KeySpec<Number> = aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Number("id")
        }
        """.trimIndent())

        // GetTable
        assertContains(schemaContents, """
        /**
         * Returns a reference to a table named [name] containing items representing [User]
         */
        public fun DynamoDbMapper.getUserTable(name: String): Table.PartitionKey<User, Int> = getTable(name, UserSchema)
        """.trimIndent())
    }

    @Test
    fun testBuilderNotRequired() {
        createClassFile("BuilderNotRequired")

        val result = runner
            .withArguments("--info", "build")
            .build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)


        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/mapper/schemas/BuilderNotRequiredSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Assert a builder is not generated, because it contains all mutable members with default values and a zero-arg constructor
        assertFalse(schemaContents.contains("public class BuilderNotRequiredBuilder {"))

        // Assert that the class itself is used as a builder
        assertContains(schemaContents, """
        public object BuilderNotRequiredConverter : ItemConverter<BuilderNotRequired> by SimpleItemConverter(
            builderFactory = { BuilderNotRequired() },
            build = { this },
            descriptors = arrayOf(
                AttributeDescriptor(
                    "id",
                    BuilderNotRequired::id,
                    BuilderNotRequired::id::set,
                    IntConverter
                ),
                AttributeDescriptor(
                    "fName",
                    BuilderNotRequired::givenName,
                    BuilderNotRequired::givenName::set,
                    StringConverter
                ),
                AttributeDescriptor(
                    "lName",
                    BuilderNotRequired::surname,
                    BuilderNotRequired::surname::set,
                    StringConverter
                ),
                AttributeDescriptor(
                    "age",
                    BuilderNotRequired::age,
                    BuilderNotRequired::age::set,
                    IntConverter
                ),
            ),
        )
        """.trimIndent())
    }

    @Test
    fun testGenerateBuilderOption() {
        val pluginConfiguration = """
        import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
        dynamoDbMapper {
            generateBuilderClasses = GenerateBuilderClasses.ALWAYS
        }
        
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("BuilderNotRequired")

        val result = runner
            .withArguments("--info", "build")
            .build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)


        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/mapper/schemas/BuilderNotRequiredSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Assert a builder is still generated, because we configured GenerateBuilderClasses.ALWAYS
        assertContains(schemaContents, """
        /**
         * A DSL-style builder for instances of [BuilderNotRequired]
         */
        public class BuilderNotRequiredBuilder {
            public var id: Int? = null
            public var givenName: String? = null
            public var surname: String? = null
            public var age: Int? = null
        
            public fun build(): BuilderNotRequired {
                val id = requireNotNull(id) { "Missing value for id" }
                val givenName = requireNotNull(givenName) { "Missing value for givenName" }
                val surname = requireNotNull(surname) { "Missing value for surname" }
                val age = requireNotNull(age) { "Missing value for age" }
        
                return BuilderNotRequired(
                    id,
                    givenName,
                    surname,
                    age,
                )
            }
        }
        """.trimIndent())
    }

    @Test
    fun testVisibilityOption() {
        val pluginConfiguration = """
        import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.Visibility
        dynamoDbMapper {
            visibility = Visibility.INTERNAL
        }
        
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner
            .withArguments("--info", "build")
            .build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/mapper/schemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(schemaContents, "internal class UserBuilder")
        assertContains(schemaContents, "internal object UserConverter")
        assertContains(schemaContents, "internal object UserSchema")
        assertContains(schemaContents, "internal fun DynamoDbMapper.getUserTable")
    }

    @Test
    fun testGenerateGetTableFunctionOption() {
        val pluginConfiguration = """
        dynamoDbMapper {
            generateGetTableExtension = false
        }
        
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner
            .withArguments("--info", "build")
            .build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/mapper/schemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(schemaContents, "public class UserBuilder")
        assertContains(schemaContents, "public object UserConverter")
        assertContains(schemaContents, "public object UserSchema")
        assertFalse(schemaContents.contains( "public fun DynamoDbMapper.getUserTable"))
    }

    @Test
    fun testRelativeDestinationPackage() {
        val pluginConfiguration = """
        import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
        dynamoDbMapper {
            destinationPackage = DestinationPackage.RELATIVE("hello.moto")
        }
        
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner
            .withArguments("--info", "build")
            .build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/hello/moto/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(schemaContents, "package org.example.hello.moto")
    }

    @Test
    fun testAbsoluteDestinationPackage() {
        val pluginConfiguration = """
        import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
        dynamoDbMapper {
            destinationPackage = DestinationPackage.ABSOLUTE("absolutely.my.`package`")
        }
        
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner
            .withArguments("--info", "build")
            .build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/absolutely/my/`package`/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(schemaContents, "package absolutely.my.`package`")
    }
}
