FROM openjdk:11

# Build the app first (from ecs-credentials directory)
# `./gradlew build`
# OR
# ./gradlew build --include-build ../../../
COPY ./build/libs/app-1.0-SNAPSHOT-all.jar /usr/src/testapp/app.jar
WORKDIR /usr/src/testapp
CMD ["java", "-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG", "-Dorg.slf4j.simpleLogger.showDateTime=true", "-cp", "app.jar", "aws.sdk.kotlin.test.MainKt"]
