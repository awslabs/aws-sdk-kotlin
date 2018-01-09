package software.amazon.awssdk.kotlin.codegen.poet

import software.amazon.awssdk.kotlin.codegen.GenerationException
import com.squareup.kotlinpoet.*
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.time.Instant

class PoetExtensions(val basePackage: String, val javaSdkBasePackage: String) {
    val modelPackage = "$basePackage.model"
    val transformPackage = "$basePackage.transform"

    fun modelClass(type: String): ClassName = ClassName(modelPackage, type)

    val javaSdkModelPackage = "$javaSdkBasePackage.model"

    fun javaSdkModelClass(type: String): ClassName = ClassName(javaSdkModelPackage, type)

    fun javaSdkClientClass(type: String): ClassName = ClassName(javaSdkBasePackage, type)

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
                "java.nio.ByteBuffer" to ByteBuffer::class.asClassName()
        )

        @JvmStatic
        fun classNameForType(type: String): ClassName = simpleTypeMap.getOrElse(type, {
            throw GenerationException("Unknown simple type: $type")
        })
    }
}

