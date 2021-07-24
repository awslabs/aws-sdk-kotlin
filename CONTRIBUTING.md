# Contributing Guidelines

Thank you for your interest in contributing to the AWS SDK for Kotlin. Whether it's a bug report, new feature, correction, or additional 
documentation, we greatly value feedback and contributions from our community.

Please read through this document before submitting any issues or pull requests to ensure we have all the necessary 
information to effectively respond to your bug report or contribution.

## The AWS SDK for Kotlin is built from multiple GitHub repositories

1. This repository ([awslabs/aws-sdk-kotlin](https://github.com/awslabs/aws-sdk-kotlin))
   
This repository contains code generation for AWS specific services, and a corresponding runtime to support the generated code.

AWS service clients are generated from [Smithy](https://awslabs.github.io/smithy/) models. Much of the code generation is AWS agnostic though.
As such the code in `aws-sdk-kotlin` is a layer on top of generic Smithy based code generation tooling.


2. Smithy Kotlin Codegen repo ([awslabs/smithy-kotlin](https://github.com/awslabs/smithy-kotlin))

The `smithy-kotlin` repository contains the generic Smithy code generation tools for Kotlin.

If you want to contribute by diving into the codegen machinery and helping develop the SDK please refer to the [contributing guide](https://github.com/awslabs/smithy-kotlin/blob/main/CONTRIBUTING.md) in that repo.


## Reporting Bugs/Feature Requests

We welcome you to use the GitHub issue tracker to report bugs or suggest features.

When filing an issue, please check [existing open](https://github.com/aws-samples/aws-sdk-kotlin/issues), or [recently closed](https://github.com/aws-samples/aws-sdk-kotlin/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aclosed%20), issues to make sure somebody else hasn't already 
reported the issue. Please try to include as much information as you can. Details like these are incredibly useful:

* A reproducible test case or series of steps
* The version of our code being used
* Any modifications you've made relevant to the bug
* Anything unusual about your environment or deployment


## Contributing via Pull Requests
Contributions via pull requests are much appreciated. Before sending us a pull request, please ensure that:

1. You are working against the latest source on the *main* branch.
2. You check existing open, and recently merged, pull requests to make sure someone else hasn't addressed the problem already.
3. You open an issue to discuss any significant work - we would hate for your time to be wasted.

To send us a pull request, please:

1. Fork the repository.
2. Modify the source; please focus on the specific change you are contributing. If you also reformat all the code, it will be hard for us to focus on your change.
3. Ensure local tests pass.
4. Commit to your fork using clear commit messages.
5. Send us a pull request, answering any default questions in the pull request interface.
6. Pay attention to any automated CI failures reported in the pull request, and stay involved in the conversation.

GitHub provides additional document on [forking a repository](https://help.github.com/articles/fork-a-repo/) and 
[creating a pull request](https://help.github.com/articles/creating-a-pull-request/).


## Finding contributions to work on
Looking at the existing issues is a great way to find something to contribute on. As our projects, by default, use the default GitHub issue labels ((enhancement/bug/duplicate/help wanted/invalid/question/wontfix), looking at any ['help wanted'](https://github.com/aws-samples/aws-sdk-kotlin/labels/help%20wanted) issues is a great place to start. 


## Code of Conduct
This project has adopted the [Amazon Open Source Code of Conduct](https://aws.github.io/code-of-conduct). 
For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq) or contact 
opensource-codeofconduct@amazon.com with any additional questions or comments.

## Security Issues

If you discover a potential security issue in this project we ask that you **do not** create a public GitHub issue.

Please refer to our [security policy](https://github.com/awslabs/aws-sdk-kotlin/security/policy) for how to notify us.

## Licensing

See the [LICENSE](https://github.com/aws-samples/aws-sdk-kotlin/blob/main/LICENSE) file for our project's licensing. We will ask you confirm the licensing of your contribution.

We may ask you to sign a [Contributor License Agreement (CLA)](http://en.wikipedia.org/wiki/Contributor_License_Agreement) for larger changes.
