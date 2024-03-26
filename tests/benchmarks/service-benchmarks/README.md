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

## Baseline as of 8/8/2023

The following benchmark run serves as a baseline for future runs:

### Environment

| Hardware type  | Operating system | SDK version     |
|----------------|------------------|-----------------|
| EC2 m5.4xlarge | Amazon Linux 2   | 0.30.0-SNAPSHOT |

### Results

|                       | Overhead (ms) |    n |   min |   avg |   med |   p90 |    p99 |    max |
|:----------------------|--------------:|-----:|------:|------:|------:|------:|-------:|-------:|
| **S3**                |               |      |       |       |       |       |        |        |
| —HeadObject           |               | 1715 | 0.334 | 0.561 | 0.379 | 0.521 |  3.149 | 20.071 |
| —PutObject            |               |  739 | 0.306 | 0.492 | 0.337 | 0.389 |  7.958 | 16.556 |
| **SNS**               |               |      |       |       |       |       |        |        |
| —GetTopicAttributes   |               | 3041 | 0.235 | 0.494 | 0.354 | 0.461 |  2.964 | 17.129 |
| —Publish              |               | 1001 | 0.199 | 0.394 | 0.224 | 0.420 |  1.262 | 16.160 |
| **STS**               |               |      |       |       |       |       |        |        |
| —AssumeRole           |               | 1081 | 0.273 | 0.419 | 0.349 | 0.485 |  0.622 | 14.781 |
| —GetCallerIdentity    |               | 4705 | 0.157 | 0.242 | 0.184 | 0.217 |  0.414 | 13.459 |
| **CloudWatch**        |               |      |       |       |       |       |        |        |
| —GetMetricData        |               | 1500 | 0.174 | 1.352 | 0.219 | 3.239 | 13.830 | 15.193 |
| —PutMetricData        |               | 2452 | 0.133 | 1.194 | 0.144 | 1.911 | 13.007 | 14.862 |
| **CloudWatch Events** |               |      |       |       |       |       |        |        |
| —DescribeEventBus     |               | 1500 | 0.156 | 0.290 | 0.187 | 0.238 |  0.530 | 18.934 |
| —PutEvents            |               | 4577 | 0.152 | 0.293 | 0.176 | 0.378 |  3.921 | 10.022 |
| **DynamoDB**          |               |      |       |       |       |       |        |        |
| —GetItem              |               | 4223 | 0.135 | 0.154 | 0.148 | 0.164 |  0.216 |  2.415 |
| —PutItem              |               | 3059 | 0.130 | 0.154 | 0.145 | 0.178 |  0.193 |  1.771 |
| **Pinpoint**          |               |      |       |       |       |       |        |        |
| —GetEndpoint          |               |  555 | 0.220 | 0.401 | 0.406 | 0.452 |  0.506 |  6.606 |
| —PutEvents            |               |  415 | 0.242 | 0.400 | 0.420 | 0.466 |  0.619 |  2.762 |

### S3 Express
S3 Express benchmarks were ran separately.

| Hardware type  | Operating system  | SDK version |
|----------------|-------------------|-------------|
| EC2 m5.4xlarge | Amazon Linux 2023 | 1.0.66      |

|               | E2E Duration (ms) |    n |   min |   avg |   med |   p90 |   p99 |    max |
|:--------------|------------------:|-----:|------:|------:|------:|------:|------:|-------:|
| **S3Express** |                   |      |       |       |       |       |       |        |
| —PutObject    |                   | 1950 | 7.240 | 7.487 | 7.455 | 7.617 | 7.886 | 21.096 |
| —GetObject    |                   | 3402 | 4.049 | 4.188 | 4.141 | 4.243 | 4.470 | 20.537 |

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
