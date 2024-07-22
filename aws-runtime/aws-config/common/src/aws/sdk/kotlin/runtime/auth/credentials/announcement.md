An upcoming release of the **AWS SDK for Kotlin** will change the order of credentials resolution for the [default credentials provider chain](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/credential-providers.html#default-credential-provider-chain).

# Release date

This feature will ship with the **v1.3.x** release planned for **xx/xx/2024**.

# What's changing

The Kotlin SDK will be changing the order in which credentials are resolved when using the default credentials provider chain. If you don't use the default credentials provider chain, this change should not affect you.

If you _do_ use the default credentials provider chain, you will need to setup your credentials considering the new order of resolution along with your upgrade to AWS SDK for Kotlin **v1.3.x**.

The current resolution order is the following:

1. Environment variables
2. **_Shared credentials and config files_**
3. **_AWS STS web identity (including Amazon Elastic Kubernetes Service (Amazon EKS))_**
4. Amazon ECS container credentials
5. Amazon EC2 Instance Metadata Service

The new order will be:

1. Environment variables
2.  **_AWS STS web identity (including Amazon Elastic Kubernetes Service (Amazon EKS))_**
3. **_Shared credentials and config files_**
4. Amazon ECS container credentials
5. Amazon EC2 Instance Metadata Service

The [default credentials provider chain documentation](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/credential-providers.html#default-credential-provider-chain) contains more details on each credential source

# How to migrate

1. Upgrade all of your AWS SDK for Kotlin dependencies to **v.1.3.x**.
7. Verify that the changes to the default credentials provider chain didn't cause any issues for your program.
8. If there are issues make sure to look at the new order in which credentials are resolved and make changes as necessary.

# Feedback

If you have any questions concerning this change, please feel free to engage with us in this discussion. If you encounter a bug with these changes, please [file an issue](https://github.com/awslabs/aws-sdk-kotlin/issues/new/choose).
