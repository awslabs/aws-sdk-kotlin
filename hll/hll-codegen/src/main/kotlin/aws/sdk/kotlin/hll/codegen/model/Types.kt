package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * A container object for various [Type] instances
 */
@InternalSdkApi
public object Types {
    @InternalSdkApi
    public object Kotlin {
        public val Number: TypeRef = kotlin("Number")
        public val OptIn: TypeRef = kotlin("OptIn")
        public val String: TypeRef = kotlin("String")
        public val StringNullable: TypeRef = String.nullable() as TypeRef

        /**
         * Creates a [TypeRef] for a generic [List]
         * @param element The type of elements in the list
         */
        public fun list(element: Type): TypeRef = TypeRef(Pkg.Kotlin.Collections, "List", listOf(element))

        /**
         * Creates a [TypeRef] for a named Kotlin type (e.g., `String`)
         */
        public fun kotlin(name: String): TypeRef = TypeRef(Pkg.Kotlin.Base, name)

        /**
         * Creates a [TypeRef] for a generic [Map]
         * @param key The type of keys in the map
         * @param value The type of values in the map
         */
        public fun map(key: Type, value: Type): TypeRef = TypeRef(Pkg.Kotlin.Collections, "Map", listOf(key, value))

        /**
         * Creates a [TypeRef] for a generic [Map] with [String] keys
         * @param value The type of values in the map
         */
        public fun stringMap(value: Type): TypeRef = map(String, value)

        @InternalSdkApi
        public object Jvm {
            public val JvmName: TypeRef = TypeRef(Pkg.Kotlin.Jvm, "JvmName")
        }
    }

    @InternalSdkApi
    public object Kotlinx {
        @InternalSdkApi
        public object Coroutines {
            @InternalSdkApi
            public object Flow {
                public val flow: TypeRef = TypeRef(Pkg.Kotlinx.Coroutines.Flow, "flow")

                /**
                 * Creates a [TypeRef] for a generic `Flow`
                 * @param element The type of elements in the flow
                 */
                public fun flow(element: Type): TypeRef =
                    TypeRef(Pkg.Kotlinx.Coroutines.Flow, "Flow", listOf(element))

                public val transform: TypeRef = TypeRef(Pkg.Kotlinx.Coroutines.Flow, "transform")
            }
        }
    }
}
