# AWS SDK for Kotlin


## License

This library is licensed under the Apache 2.0 License. 


## Development

### Generate SDK(s)

Generated sources are not checked into the repository, you first have to generate the clients before you can build them.


```
./gradlew :codegen:sdk-codegen:bootstrap
```

NOTE: This task will respect the AWS services specified by project properties. See options below.
NOTE: To re-run codegen for the same set of services multiple times add the `--rerun-tasks` flag.


After generating the services you care about they are available to build:

e.g.
```
./gradlew :services:lambda:build
```


Where the task follows the pattern: `:services:SERVICE:build`

To see list of all projects run `./gradlew projects`


### Build properties

You can define a `local.properties` config file at the root of the project to modify build behavior. 

An example config with the various properties is below:

```
# comma separated list of paths to `includeBuild()`
# This is useful for local development of smithy-kotlin in particular 
compositeProjects=../smithy-kotlin

# comma separated list of services to generate from sdk-codegen. When not specified all services are generated
# service names match the filenames in the models directory `service.VERSION.json`
aws.services=lambda
```


##### Generating a single service
See the local.properties definition above to specify this in a config file.

```
./gradlew -Paws.services=lambda  :codegen:sdk-codegen:bootstrap
```

##### Testing Locally
Testing generated services generally requires publishing artifacts (e.g. client-runtime) of `smithy-kotlin`, `aws-crt-kotlin`, and `aws-sdk-kotin` to maven local.
