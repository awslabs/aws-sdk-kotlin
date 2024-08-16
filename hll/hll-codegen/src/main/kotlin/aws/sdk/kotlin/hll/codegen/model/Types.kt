package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.hll.codegen.util.Pkg

/**
 * A container object for various [Type] instances
 */
object Types {
    object Kotlin {
        val String = kotlin( "String")
        val Number = kotlin("Number")
        val StringNullable = String.nullable()

        /**
         * Creates a [TypeRef] for a generic [List]
         * @param element The type of elements in the list
         */
        fun list(element: Type) = TypeRef(Pkg.Kotlin.Collections, "List", listOf(element))

        /**
         * Creates a [TypeRef] for a named Kotlin type (e.g., `String`)
         */
        fun kotlin(name: String) = TypeRef(Pkg.Kotlin.Base, name)

        /**
         * Creates a [TypeRef] for a generic [Map]
         * @param key The type of keys in the map
         * @param value The type of values in the map
         */
        fun map(key: Type, value: Type) = TypeRef(Pkg.Kotlin.Collections, "Map", listOf(key, value))

        /**
         * Creates a [TypeRef] for a generic [Map] with [String] keys
         * @param value The type of values in the map
         */
        fun stringMap(value: Type) = map(String, value)
    }
}
