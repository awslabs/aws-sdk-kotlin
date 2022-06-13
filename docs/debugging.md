# Debugging 

## Logging

### JVM

For JVM targets, the Kotlin SDK uses the `slf4j` logger.  The build configuration can be updated to enable log output.

While any `slf4j`-compatible log library may be used, here is an example to enable log output from the SDK in JVM 
programs via Log4j2:

#### Gradle dependencies

Add the following dependencies via Gradle:

```
implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
implementation("org.apache.logging.log4j:log4j-core:2.17.2")
implementation("org.apache.logging.log4j:log4j-api:2.17.2")
```

#### Log4j2 configuration file

Create a file named **log4j2.xml** in your **resources** directory (e.g., **\<project-dir>/src/main/resources**). Add
the following XML configuration to the file:

```xml
<Configuration status="ERROR">
    <Appenders>
        <Console name="Out">
            <PatternLayout pattern="%d{YYYY-MM-dd HH:mm:ss} %-5p %c:%L - %encode{%m}{CRLF}%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Out"/>
        </Root>
    </Loggers>
</Configuration>
```

For more information on how to customize logging, see
[Log4j2's Configuration guide](https://logging.apache.org/log4j/2.x/manual/configuration.html#).

To view low-level request and response log output and the time of the log entry, change the appender level to `trace`:

```xml
<Root level="trace">
```

#### Unit Tests

To enable or customize logging in JVM unit tests, add a **log4j2-test.xml** file to the **resources** directory.

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
