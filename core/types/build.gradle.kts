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
            dependencies {
                api(kotlin("stdlib-common"))
                api(kotlin("stdlib-jdk8"))

                api("com.soywiz.korlibs.klock:klock:1.8.6")
                api("com.squareup.okio:okio:2.4.3")
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