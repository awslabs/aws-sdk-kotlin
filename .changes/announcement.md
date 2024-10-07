An upcoming release of the **AWS SDK for Kotlin** will change the order of 
credentials resolution for the [default credentials provider chain](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/credential-providers.html#default-credential-provider-chain)
and the order of credentials resolution for the AWS shared config files.

# Release date

This feature will ship with the **v1.4.x** release on xx/xx/xxxx.

# What's changing

The SDK will be changing the order in which credentials are resolved when
using the default credentials provider chain. The new order will be:

1. System properties
2. Environment variables
3. Assume role with web identity token
4. Shared credentials and config files (profile)
5. Amazon ECS container credentials
6. Amazon EC2 Instance Metadata Service

The [default credentials provider chain documentation](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/credential-providers.html#default-credential-provider-chain) 
contains more details on each credential source.

The SDK will also be changing the order in which credentials are resolved from
in the shared credentials and config files. The new order will be:

1. Static credentials
2. Assume role with source profile OR assume role with named provider (mutually exclusive)
3. Web identity token
4. SSO session
5. Legacy SSO
6. Process

# How to migrate

1. Upgrade all of your AWS SDK for Kotlin dependencies to **v.1.4.x**.
2. Verify that the changes to the default credentials provider chain and credentials files do not introduce any issues in your program.
3. If issues arise review the new credentials resolution order and adjust your configuration as needed.

# Feedback

If you have any questions concerning this change, please feel free to engage 
with us in this discussion. If you encounter a bug with these changes, please 
[file an issue](https://github.com/awslabs/aws-sdk-kotlin/issues/new/choose).