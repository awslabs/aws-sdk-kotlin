package software.aws.kotlin.utils

val isNode by lazy {
    js(
        "typeof process !== 'undefined' && process.versions != null && process.versions.node != null"
    ) as Boolean
}