package software.amazon.awssdk.kotlin.codegen.poet

import com.squareup.kotlinpoet.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

object Utils {
    val simpleTypeMap = mapOf(
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
            "Instant" to Instant::class.asClassName()
    )
}