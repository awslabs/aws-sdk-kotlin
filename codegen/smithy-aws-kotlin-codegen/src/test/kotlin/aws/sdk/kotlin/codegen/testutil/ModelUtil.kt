package aws.sdk.kotlin.codegen.testutil

import software.amazon.smithy.kotlin.codegen.test.prependNamespaceAndService
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.Model

internal fun model(serviceName: String): Model =
    """
        @http(method: "PUT", uri: "/foo")
        operation Foo { }
        
        @http(method: "POST", uri: "/bar")
        operation Bar { }
    """
        .prependNamespaceAndService(operations = listOf("Foo", "Bar"), serviceName = serviceName)
        .toSmithyModel()
