package aws.sdk.kotlin.tests.codegen.unionexample

import aws.sdk.kotlin.test.unions.model.Foo
import kotlin.test.Test

class UnionTest {
    @Test
    fun `try code generating a union with a member that has the same name as it, then use it`() {
        Foo.Foo(true).asFoo()
    }
}
