plugins {
    `dokka-convention`
}

dependencies {
    dokka(project("dynamodb-mapper"))
    dokka(project("dynamodb-mapper-annotations"))
}
