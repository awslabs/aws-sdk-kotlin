package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Ignore
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaGeneratorPluginTest {
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: File

    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var runner: GradleRunner

    private fun getResource(resourceName: String): String = checkNotNull(this::class.java.getResource(resourceName)?.readText()) { "Could not read $resourceName" }
    private val kotlinVersion = getResource("kotlin-version.txt")
    private val sdkVersion = getResource("sdk-version.txt")

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts").also { it.writeText("") }

        buildFile = File(testProjectDir, "build.gradle.kts").also { it.writeText("") }

        // Apply the plugin and necessary dependencies
        val buildFileContent = """
        repositories {
            mavenCentral()
            mavenLocal()
        }
        
        plugins {
            id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
            id("aws.sdk.kotlin.hll.dynamodbmapper.schema.generator")
        }
        
        dependencies {
            implementation("aws.sdk.kotlin:dynamodb-mapper:$sdkVersion")
            implementation("aws.sdk.kotlin:dynamodb-mapper-annotations:$sdkVersion")
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
            .withArguments("--info", "build")
    }

    private fun File.prependText(text: String) {
        val existingContent = readText()
        writeText(text)
        appendText(existingContent)
    }

    private fun createClassFile(className: String, path: String = "src/main/kotlin/org/example") {
        val classFile = File(testProjectDir, "$path/$className.kt")
        classFile.ensureParentDirsCreated()
        classFile.createNewFile()
        classFile.writeText(getResource("/$className.kt"))
    }

    @Ignore
    @Test
    fun testDefaultOptions() {
        createClassFile("User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/aws/sdk/kotlin/hll/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Builder
        assertContains(
            schemaContents,
            """
            /**
             * A DSL-style builder for instances of [User]
             */
             class UserBuilder {
                 var id: Int? = null
                 var givenName: String? = null
                 var surname: String? = null
                 var age: Int? = null
            
                 fun build(): User {
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
            """.trimIndent(),
        )

        // Converter
        assertContains(
            schemaContents,
            """
        object UserConverter : ItemConverter<User> by SimpleItemConverter(
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
            """.trimIndent(),
        )

        // Schema
        assertContains(
            schemaContents,
            """
        object UserSchema : ItemSchema.PartitionKey<User, Int> {
            override val converter : UserConverter = UserConverter
            override val partitionKey: KeySpec<Number> = aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Number("id")
        }
            """.trimIndent(),
        )

        // GetTable
        assertContains(
            schemaContents,
            """
            /**
             * Returns a reference to a table named [name] containing items representing [User]
             */
             fun DynamoDbMapper.getUserTable(name: String): Table.PartitionKey<User, Int> = getTable(name, UserSchema)
            """.trimIndent(),
        )
    }

    @Test
    fun testBuilderNotRequired() {
        createClassFile("BuilderNotRequired")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/aws/sdk/kotlin/hll/dynamodbmapper/generatedschemas/BuilderNotRequiredSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Assert a builder is not generated, because it contains all mutable members with default values and a zero-arg constructor
        assertFalse(schemaContents.contains("public class BuilderNotRequiredBuilder {"))

        // Assert that the class itself is used as a builder
        assertContains(
            schemaContents,
            """
        object BuilderNotRequiredConverter : ItemConverter<BuilderNotRequired> by SimpleItemConverter(
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
            """.trimIndent(),
        )
    }

    @Ignore
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

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/aws/sdk/kotlin/hll/dynamodbmapper/generatedschemas/BuilderNotRequiredSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Assert a builder is still generated, because we configured GenerateBuilderClasses.ALWAYS
        assertContains(
            schemaContents,
            """
            /**
             * A DSL-style builder for instances of [BuilderNotRequired]
             */
             class BuilderNotRequiredBuilder {
                 var id: Int? = null
                 var givenName: String? = null
                 var surname: String? = null
                 var age: Int? = null
            
                 fun build(): BuilderNotRequired {
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
            """.trimIndent(),
        )
    }

    @Test
    fun testVisibilityOption() {
        val pluginConfiguration = """
        import aws.sdk.kotlin.hll.codegen.rendering.Visibility
        dynamoDbMapper {
            visibility = Visibility.INTERNAL
        }
        
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/aws/sdk/kotlin/hll/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // All codegenerated constructs should be `internal`
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

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/aws/sdk/kotlin/hll/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // getUserTable should not be generated
        assertContains(schemaContents, "class UserBuilder")
        assertContains(schemaContents, "object UserConverter")
        assertContains(schemaContents, "object UserSchema")
        assertFalse(schemaContents.contains("fun DynamoDbMapper.getUserTable"))
    }

    @Test
    fun testRelativeDestinationPackage() {
        val pluginConfiguration = """
        import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
        dynamoDbMapper {
            destinationPackage = DestinationPackage.Relative("hello.moto")
        }
        
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner.build()
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
            destinationPackage = DestinationPackage.Absolute("absolutely.my.`package`")
        }
        
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/absolutely/my/`package`/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(schemaContents, "package absolutely.my.`package`")
    }

    @Test
    fun testGeneratedItemConverter() {
        buildFile.appendText(
            """
            dependencies {
                testImplementation(kotlin("test")) 
            }

            """.trimIndent(),
        )

        createClassFile("User")

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/UserTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/tests/UserTest.kt"))

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)
        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/aws/sdk/kotlin/hll/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }
}
