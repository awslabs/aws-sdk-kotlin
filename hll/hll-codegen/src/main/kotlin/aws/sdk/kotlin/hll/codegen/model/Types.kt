package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.hll.codegen.util.Pkg

/**
 * A container object for various [Type] instances
 */
object Types {
    object Smithy {
        val Instant = TypeRef("aws.smithy.kotlin.runtime.time", "Instant")
        val Url = TypeRef("aws.smithy.kotlin.runtime.net.url", "Url")
        val Document = TypeRef("aws.smithy.kotlin.runtime.content", "Document")
    }

    object Kotlin {
        val ByteArray = kotlin("ByteArray")
        val Boolean = kotlin("Boolean")
        val Number = kotlin("Number")
        val String = kotlin("String")
        val StringNullable = String.nullable()
        val Char = kotlin("Char")
        val CharArray = kotlin("CharArray")
        val Byte = kotlin("Byte")
        val Short = kotlin("Short")
        val Int = kotlin("Int")
        val Long = kotlin("Long")
        val Float = kotlin("Float")
        val Double = kotlin("Double")
        val UByte = kotlin("UByte")
        val UInt = kotlin("UInt")
        val ULong = kotlin("ULong")
        val UShort = kotlin("UShort")

        object Collections {
            val Set = TypeRef(Pkg.Kotlin.Collections, "Set")
            val List = TypeRef(Pkg.Kotlin.Collections, "List")
            val Map = TypeRef(Pkg.Kotlin.Collections, "Map")
        }

        /**
         * Creates a [TypeRef] for a generic [List]
         * @param element The type of elements in the list
         */
        fun list(element: TypeRef) = TypeRef(Pkg.Kotlin.Collections, "List", listOf(element))

        /**
         * Creates a [TypeRef] for a named Kotlin type (e.g., `String`)
         */
        fun kotlin(name: String) = TypeRef(Pkg.Kotlin.Base, name)

        /**
         * Creates a [TypeRef] for a generic [Map]
         * @param key The type of keys in the map
         * @param value The type of values in the map
         */
        fun map(key: TypeRef, value: TypeRef) = TypeRef(Pkg.Kotlin.Collections, "Map", listOf(key, value))

        /**
         * Creates a [TypeRef] for a generic [Map] with [String] keys
         * @param value The type of values in the map
         */
        fun stringMap(value: TypeRef) = map(String, value)
    }
}
