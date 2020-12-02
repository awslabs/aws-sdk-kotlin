# AWS SDK for Kotlin


## License

This library is licensed under the Apache 2.0 License. 


## Development

You can define a `local.properties` config file at the root of the project to modify build behavior. 

An example config with the various properties is below:

```
# comma separated list of paths to `includeBuild()`
# This is useful for local development of smithy-kotlin in particular 
compositeProjects=../smithy-kotlin

# comma separated list of services to generate from sdk-codegen. When not specified all services are generated
# service names match the filenames in the models directory `service.VERSION.json`
aws.services=lambda

# when generating aws services build as a standalong project or not (rootProject = true)
buildStandaloneSdk=true
```


##### Building a single service
See the local.properties definition above to specify this in a config file.

```
>> ./gradlew -Paws.services=lambda  :sdk-codegen:build
```

##### Testing Locally
Testing generated services generally requires publishing artifacts (e.g. client-runtime) of `smithy-kotlin`, `aws-crt-kotlin`, and `aws-sdk-kotin` to maven local.
