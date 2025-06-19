plugins {
    `dokka-convention`
}

dependencies {
    dokka(project(":hll:dynamodb-mapper:dynamodb-mapper"))
    dokka(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
}
