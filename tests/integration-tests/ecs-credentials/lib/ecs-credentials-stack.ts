import {CfnOutput, Stack, StackProps} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ecs from 'aws-cdk-lib/aws-ecs';

export class EcsCredentialsStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    // The code that defines your stack goes here

    const cluster = new ecs.Cluster(this, 'SdkTestCluster');
    cluster.enableFargateCapacityProviders();

    const logging = new ecs.AwsLogDriver({
      streamPrefix: "ecs-test-app"
    });

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'TaskDef', {
      memoryLimitMiB: 512,
      cpu: 256,
    });

    taskDefinition.addContainer('CredentialProviderTestContainer', {
      image: ecs.ContainerImage.fromAsset('./app'),
      logging
    });

    // stack outputs
    new CfnOutput(this, 'ClusterArn', { value: cluster.clusterArn });
    new CfnOutput(this, 'TaskArn', {value: taskDefinition.taskDefinitionArn });
    new CfnOutput(this, 'ClusterVpcId', {value: cluster.vpc.vpcId});
    new CfnOutput(this, 'ClusterVpcSubnet0', {value: cluster.vpc.privateSubnets[0].subnetId});
    new CfnOutput(this, 'LogGroupName', {value: logging.logGroup?.logGroupName!});


    // A service will try to keep at least `desiredCount` instances running. This is more of a one off task
    // const service = new ecs.FargateService(this, 'TestService', {
    //   cluster,
    //   taskDefinition,
    //   desiredCount: 1
    // });
  }
}
