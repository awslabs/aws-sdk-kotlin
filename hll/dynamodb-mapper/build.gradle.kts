plugins {
    `dokka-convention`
}

dependencies {
    dokka(rootProject.project(":hll:dynamodb-mapper:dynamodb-mapper"))
    dokka(rootProject.project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
}
