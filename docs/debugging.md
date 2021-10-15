# Debugging 

## Logging

### JVM

For JVM targets, the Kotlin SDK uses the `slf4j` logger.  The build configuration can be updated to enable log output.

While any `slf4j`-compatible log library may be used, here is an example to enable log output from the SDK in JVM 
programs:

```
implementation("org.slf4j:slf4j-simple:1.7.30")
```

To view low-level request and response log output and the time of the log entry, specify this as JVM parameters to the executing program:

```
-Dorg.slf4j.simpleLogger.defaultLogLevel=TRACE -Dorg.slf4j.simpleLogger.showDateTime=true
```

The log level can be adjusted up as needed to DEBUG, INFO, WARN, or ERROR.  [See here](http://www.slf4j.org/api/org/slf4j/impl/SimpleLogger.html) for all properties for the simple logger.

#### Unit Tests

To enable logging in JVM unit tests, the JVM properties can be passed via the Gradle `test` task.  Here is an example snippet from a `build.gradle.kts` file:

```
tasks.test {
    options {
        jvmArgs = listOf("-Dorg.slf4j.simpleLogger.defaultLogLevel=TRACE", "-Dorg.slf4j.simpleLogger.showDateTime=true")
    }
}
```

#### CRT Logs

JVM system properties for CRT related logs


| Property                    | Description                                                                                                      |
| ----------------------------| -----------------------------------------------------------------------------------------------------------------|
| `aws.crt.log.level`         | Specify log level `None`, `Fatal`, `Error`, `Warn`, `Info`, `Debug`, `Trace`                                     |
| `aws.crt.log.destination`   | The destination to log to `None`, `Stdout`, `Stderr`, `File`. By default when level is not `None` stderr is used |
| `aws.crt.log.filename`      | Redirect logs to a file                                                                                          |


## Error Metadata

The raw protocol response is usually available on exceptions if you need access to additional response details (headers, status code, etc).


```kotlin
try {
    ...
} catch(ex: AwsServiceException) {
    val httpResp = ex.sdkErrorMetadata.protocolResponse as? HttpResponse
    if (httpResp != null) {
        println(httpResp.headers)
        println(httpResp.body.readAll()?.decodeToString())
    }
}
```
