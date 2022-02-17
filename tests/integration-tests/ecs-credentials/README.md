# CDK stack for ECS credentials provider testing

This project defines a CDK stack that launches a fargate task with a role that the task can assume.

It includes the CDK stack as well as a test application for exercising the ECS credentials provider.
You could use this application/stack to test any ECS related task/functionality for the SDK. By default it
just assumes the role and prints out the information using `STS::GetCallerIdentity`. Logs are output to 
CloudWatch Logs.

# Usage

```sh
./gradlew build
cdk bootstrap <accountid>/<region>
cdk deploy --outputs-file outputs.json

# launch a single task instance
aws ecs run-task \
    --cluster $(jq -r '.EcsCredentialsStack.ClusterArn' outputs.json) \
    --task-definition $(jq -r '.EcsCredentialsStack.TaskArn' outputs.json) \
    --count 1 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[$(jq -r '.EcsCredentialsStack.ClusterVpcSubnet0' outputs.json)]}"
    
# alternatively you can just launch it in the console manually   

# NOTE: may have to wait for it to be run

# print out latest logs from task execution
export LOG_GROUP=$(jq -r '.EcsCredentialsStack.LogGroupName' outputs.json)
aws logs get-log-events --log-group-name $LOG_GROUP --log-stream-name $(aws logs describe-log-streams --log-group-name $LOG_GROUP --max-items 1 --order-by LastEventTime --descending --query "logStreams[].logStreamName" --output text | head -n 1) --query "events[].message" 

# Tear down when done
cdk destroy
```

The test application is a standalone project and uses the published versions of the SDK and runtime by default.
To override this add the `aws-sdk-kotlin` project as a composite build which will substitute dependencies 
as appropriate (e.g. to test out runtime changes or even generated services).

e.g.
```sh
./gradlew build --include-build ../../../
```

You can see dependencies with:
```sh
./gradlew app:dependencies --include-build ../../../
```

## Build and test application standalone

You can build and run the test application on your local machine. 

```
docker build -t ecs-test-app app
docker run --rm ecs-test-app
```


The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands

 * `npm run build`   compile typescript to js
 * `npm run watch`   watch for changes and compile
 * `npm run test`    perform the jest unit tests
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk synth`       emits the synthesized CloudFormation template
