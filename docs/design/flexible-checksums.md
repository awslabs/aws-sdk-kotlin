# Flexible Checksums Design

* **Type**: Design
* **Author**: Matas Lauzadis
* 
# Abstract

[Flexible checksums](https://aws.amazon.com/blogs/aws/new-additional-checksum-algorithms-for-amazon-s3/) is a feature 
that allows users and services to configure checksum operations for both HTTP requests and responses. To enable the feature, 
services will add an `httpChecksum` trait to their Smithy models. Today, only S3 uses the trait.

This document covers the design for supporting flexible checksums in the AWS SDK for Kotlin.

# Design

## Requirements

- Support the Smithy trait `httpChecksum`


## 

# Appendix

# Revision history
- 10/24/2022 - Created
