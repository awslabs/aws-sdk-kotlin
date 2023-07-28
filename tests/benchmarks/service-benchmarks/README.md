# Service benchmarks

This module is used for benchmarking the performance of generated clients against AWS services. The top 7 services (by
traffic coming from the AWS SDK for Kotlin) are tested and metrics are captured with summaries distilled after the runs
are complete

## Instructions

To run the benchmarks:
* `./gradlew :tests:benchmarks:service-benchmarks:bootstrapAll`
  This ensures that all the required service clients are bootstrapped and ready to be built. **You only need to do this
  once** in your workspace unless you clean up generated services or make a change to codegen.
* `./gradlew build`
  This builds the whole SDK.
* `./gradlew :tests:benchmarks:service-benchmarks:run`
  This runs the benchmark suite and prints the results to the console formatted as a Markdown table.

## Baseline as of 7/28/2023

The following benchmark run serves as a baseline for future runs:

### Host machine

| Hardware type  | Operating system | Date      |
|----------------|------------------|-----------|
| EC2 m5.4xlarge | Amazon Linux 2   | 7/28/2023 |

### Results

|                       | Overhead (ms) |    n |   min |   avg |   med |   p90 |    p99 |    max |
| :---                  |          ---: | ---: |  ---: |  ---: |  ---: |  ---: |   ---: |   ---: |
| **S3**                |               |      |       |       |       |       |        |        |
|   —HeadObject         |               | 1618 | 0.340 | 0.605 | 0.417 | 0.638 |  4.864 | 14.672 |
|   —PutObject          |               |  766 | 0.310 | 0.557 | 0.392 | 0.675 |  4.008 | 13.358 |
| **SNS**               |               |      |       |       |       |       |        |        |
|   —GetTopicAttributes |               | 3458 | 0.233 | 0.514 | 0.373 | 0.515 |  4.378 | 18.719 |
|   —Publish            |               | 1082 | 0.192 | 0.432 | 0.255 | 0.454 |  3.006 | 19.466 |
| **STS**               |               |      |       |       |       |       |        |        |
|   —AssumeRole         |               | 1054 | 0.269 | 0.442 | 0.349 | 0.525 |  0.844 | 19.312 |
|   —GetCallerIdentity  |               | 4202 | 0.158 | 0.270 | 0.204 | 0.287 |  0.462 | 19.110 |
| **CloudWatch**        |               |      |       |       |       |       |        |        |
|   —GetMetricData      |               | 1500 | 0.177 | 1.501 | 0.266 | 5.510 | 13.842 | 18.671 |
|   —PutMetricData      |               | 2470 | 0.131 | 1.211 | 0.143 | 3.206 | 11.461 | 15.233 |
| **CloudWatch Events** |               |      |       |       |       |       |        |        |
|   —DescribeEventBus   |               | 1500 | 0.169 | 0.380 | 0.248 | 0.449 |  3.642 | 11.034 |
|   —PutEvents          |               | 4007 | 0.159 | 0.340 | 0.210 | 0.344 |  4.881 | 12.941 |
| **DynamoDB**          |               |      |       |       |       |       |        |        |
|   —GetItem            |               | 3547 | 0.135 | 0.187 | 0.164 | 0.250 |  0.344 |  4.114 |
|   —PutItem            |               | 2659 | 0.127 | 0.181 | 0.159 | 0.246 |  0.324 |  2.353 |
| **Pinpoint**          |               |      |       |       |       |       |        |        |
|   —GetEndpoint        |               |  368 | 0.245 | 0.436 | 0.380 | 0.669 |  0.824 |  1.238 |
|   —PutEvents          |               |  297 | 0.277 | 0.376 | 0.351 | 0.505 |  0.696 |  0.717 |

## Methodology

This section describes how the benchmarks actually work at a high level:

### Selection criteria

These benchmarks select a handful of services to test against. The selection criterion is the top 7 services by traffic
coming from the AWS SDK for Kotlin (i.e., not from other SDKs, console, etc.). As of 7/28, those top 7 services are S3,
SNS, STS, CloudWatch, CloudWatch Events, DynamoDB, and Pinpoint (in descending order).

For each service, two APIs are selected roughly corresponding to a read and a write operation (e.g., S3::HeadObject is
a read operation and S3::PutObject is a write operation). Efforts are made to ensure that the APIs selected are the top
operations by traffic but alternate APIs may be selected in the case of low throttling limits, high setup complexity,
etc.

### Workflow

Benchmarks are run sequentially in a single thread. This is the high-level workflow for the benchmarks:

* For each benchmark service:
  * Instantiate a client with a [special telemetry provider](#telemetry-provider)
  * Run any necessary service-specific setup procedures (e.g., create/configure prerequisite resources)
  * For each benchmark operation:
    * Run any necessary operation-specific setup procedures (e.g., create/configure prerequisite resources)
    * Warmup the API call
    * Measure the API call
    * Aggregate operation metrics
    * Run any necessary operation-specific cleanup procedures (e.g., delete resources created in the setup step)
  * Run any necessary service-specific cleanup procedures (e.g., delete resources created in the setup step)
  * Print overall metrics summary

### Telemetry provider

A custom [benchmark-specific telemetry provider][1] is used to instrument each service client. This provider solely
handles metrics (i.e., no logging, tracing, etc.). It captures specific histogram metrics from an allowlist (currently
only `smithy.client.attempt_overhead_duration`) and aggregates them for the duration of an operation run (not including
the warmup phase). After the run is complete, the metrics are aggregated and various statistics are calculated (e.g.,
minimum, average, median, etc.).

[1]: common/src/aws/sdk/kotlin/benchmarks/service/telemetry/BenchmarkTelemetryProvider.kt
