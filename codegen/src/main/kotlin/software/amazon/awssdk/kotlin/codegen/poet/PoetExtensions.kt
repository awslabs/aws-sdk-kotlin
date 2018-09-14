package software.amazon.awssdk.kotlin.codegen.poet

import com.squareup.kotlinpoet.*
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.kotlin.codegen.CodeGenerator
import software.amazon.awssdk.kotlin.codegen.GenerationException
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.Generated

class PoetExtensions(val basePackage: String, val javaSdkBasePackage: String) {
    val modelPackage = "$basePackage.model"
    val transformPackage = "$basePackage.transform"

    fun modelClass(type: String): ClassName = ClassName(modelPackage, type)

    val javaSdkModelPackage = "$javaSdkBasePackage.model"

    fun javaSdkModelClass(type: String): ClassName = ClassName(javaSdkModelPackage, type)

    fun javaSdkClientClass(type: String): ClassName = ClassName(javaSdkBasePackage, type)

    val generated: AnnotationSpec = AnnotationSpec.builder(Generated::class)
            .addMember("%S", CodeGenerator::class.java.name)
            .build()

    companion object {
        private val simpleTypeMap = mapOf(
                "String" to String::class.asClassName(),
                "Boolean" to BOOLEAN,
                "Integer" to INT,
                "Long" to LONG,
                "Short" to SHORT,
                "Byte" to BYTE,
                "BigInteger" to BigInteger::class.asClassName(),
                "Double" to DOUBLE,
                "Float" to FLOAT,
                "BigDecimal" to BigDecimal::class.asClassName(),
                "Instant" to Instant::class.asClassName(),
                "java.time.Instant" to Instant::class.asClassName(),
                "Object" to Any::class.asClassName(),
                "ByteBuffer" to ByteBuffer::class.asClassName(),
                "java.nio.ByteBuffer" to ByteBuffer::class.asClassName(),
                "software.amazon.awssdk.core.SdkBytes" to SdkBytes::class.asClassName(),
                "SdkBytes" to SdkBytes::class.asClassName()
        )

        @JvmStatic
        fun classNameForType(type: String): ClassName = simpleTypeMap.getOrElse(type, {
            throw GenerationException("Unknown simple type: $type")
        })
    }
}

