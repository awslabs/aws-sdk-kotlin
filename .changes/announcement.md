An upcoming release of the **AWS SDK for Kotlin** will change the order of 
credentials resolution for the [default credentials provider chain](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/credential-providers.html#default-credential-provider-chain)
and the order of credentials resolution for AWS shared config files.

# Release date

This change will be included in the upcoming **v1.4.x** release, expected in the 
upcoming months.

# What's changing

The order of credentials resolution for the default credentials provider chain,
and the order of credentials resolution for AWS shared config files (profile chain). 

## Default credentials provider chain

The table below outlines the current and new order in which the SDK will
resolve credentials from the default credentials provider chain.

| # | Current Order                                                          | New Order                                                              |
|---|------------------------------------------------------------------------|------------------------------------------------------------------------|
| 1 | System properties                                                      | System properties                                                      |
| 2 | Environment variables                                                  | Environment variables                                                  |
| 3 | **Shared credentials and config files (profile credentials provider)** | **Assume role with web identity token**                                |
| 4 | **Assume role with web identity token**                                | **Shared credentials and config files (profile credentials provider)** |
| 5 | Amazon ECS container credentials                                       | Amazon ECS container credentials                                       |
| 6 | Amazon EC2 Instance Metadata Service                                   | Amazon EC2 Instance Metadata Service                                   |

The [default credentials provider chain documentation](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/credential-providers.html#default-credential-provider-chain) 
contains more details on each credential source.

## Profile chain

The table below outlines the current and new order in which the SDK will 
resolve credentials from AWS shared config files.

| # | Current Order                                                                                            | New Order                                                                                   |
|---|----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| 1 | **Assume role with source profile OR assume role with named provider (mutually exclusive)**              | **Static credentials**                                                                      |
| 2 | Web identity token                                                                                       | **Assume role with source profile OR assume role with named provider (mutually exclusive)** |
| 3 | SSO session                                                                                              | Web identity token                                                                          |
| 4 | Legacy SSO                                                                                               | SSO session                                                                                 |
| 5 | Process                                                                                                  | Legacy SSO                                                                                  |
| 6 | **Static credentials (moves up to #1 when in a source profile, shifting other credential sources down)** | Process                                                                                     |

# How to migrate

1. Upgrade all of your AWS SDK for Kotlin dependencies to **v.1.4.x**.
2. Verify that the changes to the default credentials provider chain and profile chain do not introduce any issues in your program.
3. If issues arise review the new credentials resolution order, the subsections below, and adjust your configuration as needed.

## Default credentials provider chain

You can preserve the current default credentials provider chain behavior by setting
the credentials provider to a credentials provider chain with the current order, e.g.

```kotlin
S3Client{
    credentialsProvider = CredentialsProviderChain(
        SystemPropertyCredentialsProvider(),
        EnvironmentCredentialsProvider(),
        StsWebIdentityProvider(),
        ProfileCredentialsProvider(),
        EcsCredentialsProvider(),
        ImdsCredentialsProvider(),
    )
}
```

## Profile credentials provider

The order in which credentials are resolved for shared credentials and config 
files cannot be customized. If your AWS config file(s) contain multiple valid 
credential sources within a single profile, you may need to update them to align 
with the new resolution order. For example, config file `A` should be updated to 
match config file `B`. This is necessary because static credentials will now 
take precedence and be selected before assume role credentials with a source profile. 
Similar adjustments to your configuration may be necessary to maintain current
behavior. Use the new order as a guide for any required changes.

Config file `A`
```ini
[default]
role_arn = arn:aws:iam::123456789:role/Role
source_profile = A
aws_access_key_id = 0
aws_secret_access_key = 0

[profile A]
aws_access_key_id = 1
aws_secret_access_key = 2
```

Config file `B`
```ini
[default]
role_arn = arn:aws:iam::123456789:role/Role
source_profile = A

[profile A]
aws_access_key_id = 1
aws_secret_access_key = 2
```

# Feedback

If you have any questions concerning this change, please feel free to engage 
with us in this discussion. If you encounter a bug with these changes when 
released, please [file an issue](https://github.com/awslabs/aws-sdk-kotlin/issues/new/choose).