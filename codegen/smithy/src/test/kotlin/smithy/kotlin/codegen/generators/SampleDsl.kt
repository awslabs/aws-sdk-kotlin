package smithy.kotlin.codegen.generators

/**
 * This is what a DSL might look like for building requests
 */

@DslMarker
annotation class SdkBuilderMarker

class SendEmailRequest private constructor(val destination: Destination?, val replyToAddresses: List<String>?, val message: Message?) {

    companion object {
        operator fun invoke(block: Builder.() -> Unit): SendEmailRequest {
            TODO()
        }
    }

    class Builder : SdkBuilder<SendEmailRequest> {
        var replyToAddresses: List<String>? = null
        var destination: Destination? = null
        var message: Message? = null

        fun destination(block: Destination.Builder.() -> Unit) {
        }

        fun message(block: Message.Builder.() -> Unit) {
        }

        override fun build(): SendEmailRequest {
            TODO("not implemented")
        }
    }
}

class Destination private constructor(val toAddresses: List<String>?) {

    companion object {
        operator fun invoke(block: Destination.Builder.() -> Unit): Destination {
            TODO()
        }
    }

    class Builder : SdkBuilder<Destination> {
        var toAddresses: List<String>? = null

        override fun build(): Destination {
            TODO("not implemented")
        }
    }
}

class Message private constructor(val subject: Content?, val body: Body?) {

    class Builder: SdkBuilder<Message> {
        var subject: Content? = null
        fun subject(block: Content.Builder.() -> Unit) {

        }

        var body: Body? = null

        fun body(block: Body.Builder.() -> Unit) {

        }

        override fun build(): Message {
            TODO("not implemented")
        }
    }
}

class Content private constructor(val data: String?, val charset: String?) {

    class Builder: SdkBuilder<Content> {
        var data: String? = null
        var charset: String? = null
        override fun build(): Content {
            TODO("not implemented")
        }
    }
}

class Body private constructor(val text: Content?) {

    class Builder: SdkBuilder<Body> {
        var text: Content? = null
        fun text(block: Content.Builder.() -> Unit) {

        }
        override fun build(): Body {
            TODO("not implemented")
        }

    }
}

@SdkBuilderMarker
interface SdkBuilder<T> {
    fun build(): T
}