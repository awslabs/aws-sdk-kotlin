#!/usr/bin/env zsh

# Exit code for program
exitCode=0

# Asking for service
echo ""
echo "What SDK would you like to generate?"
echo "Look here for all the services available: https://github.com/awslabs/aws-sdk-kotlin/tree/main/codegen/sdk/aws-models"
echo "Input:"
read sdk
echo "Okay, $sdk"

# Bootstrap
echo "./gradlew -Paws.services=+$sdk :codegen:sdk:bootstrap"
./gradlew -Paws.services=+$sdk :codegen:sdk:bootstrap
if [ $? -eq 0 ]; then
    echo "Command succeeded."
else
    exitCode=1
fi

# Removing special characters and the formatting
myvar="$sdk"
myvar=${myvar//[^[:alnum:]]/}

# Build
echo "./gradlew :services:$myvar:build"
./gradlew :services:$myvar:build
if [ $? -eq 0 ]; then
    echo "Command succeeded."
else
    exitCode=1
fi

# Publish to maven
echo "./gradlew publishToMavenLocal"
./gradlew publishToMavenLocal
if [ $? -eq 0 ]; then
    echo "Command succeeded."
else
    exitCode=1
fi

# Check if builds are where they're supposed to
echo "ls services/$myvar/build/libs"
ls services/$myvar/build/libs

# Finish
echo "Done!"
exit $exitCode


