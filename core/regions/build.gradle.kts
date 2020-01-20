plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    js {
        nodejs()
    }

    sourceSets {
        commonMain {
//            kotlin.srcDir("$buildDir/generated/kotlin")
            dependencies {
                api(kotlin("stdlib-common"))
                api(kotlin("stdlib-jdk8"))

                api(project(":utils"))
            }
        }

        commonTest {
            dependencies {
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
            }
        }

        jvm {
            compilations {
                "main" {
                    dependencies {
                        implementation(kotlin("stdlib-jdk8"))
                    }
                }

                "test" {
                    dependencies {
                        api(kotlin("test-junit"))
                    }
                }
            }
        }

        js {
            compilations {
                "main" {
                    dependencies {
                        api(kotlin("stdlib-js"))
                    }
                }

                "test" {
                    dependencies {
                        api(kotlin("test-js"))
                    }
                }
            }
        }

        sourceSets.all {
            languageSettings.apply {
                useExperimentalAnnotation( "kotlin.time.ExperimentalTime")
            }
        }
    }
}