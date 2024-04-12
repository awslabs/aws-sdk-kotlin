#!/bin/bash

counter=0

while true; do
  ./gradlew :services:s3:jvmTest --tests "aws.sdk.kotlin.services.s3.express.DefaultS3ExpressCredentialsProviderTest.testAsyncRefreshDebounce"
  if [ $? -ne 0 ]; then
    echo "Command failed after $counter executions with exit code $?"
    break
  else
    ((counter++))
  fi
done