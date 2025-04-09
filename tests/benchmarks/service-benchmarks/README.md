# Service benchmarks

This module is used for benchmarking the performance of generated clients against AWS services. The top 7 services (by
traffic coming from the AWS SDK for Kotlin) are tested and metrics are captured with summaries distilled after the runs
are complete

## Instructions

Ensure all services, including `iam`, have been generated before proceeding with the benchmarks. To run the benchmarks:
* `./gradlew build`
  This builds the whole SDK.
* `./gradlew :tests:benchmarks:service-benchmarks:run`
  This runs the benchmark suite and prints the results to the console formatted as a Markdown table.

## Baseline as of 3/26/2024

The following benchmark run serves as a baseline for future runs:

### Environment

| Hardware type  | Operating system | SDK version     |
|----------------|------------------|-----------------|
| EC2 m5.4xlarge | Amazon Linux 2   | 1.1.4 |

### Results

|                       | Overhead (ms) |    n |   min |   avg |   med |   p90 |   p99 |    max |
| :---                  |          ---: | ---: |  ---: |  ---: |  ---: |  ---: |  ---: |   ---: |
| **S3**                |               |      |       |       |       |       |       |        |
|   —HeadObject         |               | 1637 | 0.445 | 0.628 | 0.517 | 0.655 | 6.427 | 10.388 |
|   —PutObject          |               |  741 | 0.442 | 0.624 | 0.517 | 0.591 | 5.214 | 11.596 |
| **SNS**               |               |      |       |       |       |       |       |        |
|   —GetTopicAttributes |               | 4269 | 0.294 | 0.502 | 0.392 | 0.565 | 6.191 | 29.866 |
|   —Publish            |               | 1089 | 0.255 | 0.428 | 0.337 | 0.384 | 3.072 |  9.253 |
| **STS**               |               |      |       |       |       |       |       |        |
|   —AssumeRole         |               | 1273 | 0.290 | 0.444 | 0.402 | 0.524 | 0.596 |  7.902 |
|   —GetCallerIdentity  |               | 7039 | 0.225 | 0.286 | 0.258 | 0.286 | 0.360 | 11.254 |
| **CloudWatch**        |               |      |       |       |       |       |       |        |
|   —GetMetricData      |               | 1500 | 0.245 | 0.869 | 0.325 | 4.129 | 5.988 |  6.793 |
|   —PutMetricData      |               | 3238 | 0.191 | 0.654 | 0.221 | 3.313 | 4.846 |  9.071 |
| **CloudWatch Events** |               |      |       |       |       |       |       |        |
|   —DescribeEventBus   |               | 1500 | 0.223 | 0.395 | 0.312 | 0.498 | 4.932 | 10.820 |
|   —PutEvents          |               | 7224 | 0.230 | 0.323 | 0.271 | 0.312 | 4.112 |  6.740 |
| **DynamoDB**          |               |      |       |       |       |       |       |        |
|   —GetItem            |               | 5254 | 0.210 | 0.251 | 0.246 | 0.268 | 0.293 |  3.347 |
|   —PutItem            |               | 3361 | 0.205 | 0.268 | 0.263 | 0.301 | 0.323 |  2.693 |
| **S3Express**         |               |      |       |       |       |       |       |        |
|   —PutObject          |               | 2077 | 0.659 | 0.722 | 0.709 | 0.734 | 0.772 |  7.732 |
|   —GetObject          |               | 3458 | 0.275 | 0.316 | 0.301 | 0.328 | 0.363 |  9.233 |
| **Secrets Manager**   |               |      |       |       |       |       |       |        |
|   —GetSecretValue     |               | 1206 | 0.282 | 0.434 | 0.375 | 0.434 | 3.829 |  7.043 |
|   —PutSecretValue     |               |  435 | 0.281 | 0.413 | 0.352 | 0.398 | 3.217 |  6.679 |

## Methodology

This section describes how the benchmarks actually work at a high level:

### Selection criteria

These benchmarks select a handful of services to test against. The selection criterion is the top 7 services by traffic
coming from the AWS SDK for Kotlin (i.e., not from other SDKs, console, etc.). As of 7/28, those top 7 services are S3,
SNS, STS, CloudWatch, CloudWatch Events, DynamoDB, and Pinpoint (in descending order). However, Pinpoint has strict 
throttles that make benchmarking impossible, so Secrets Manager is selected instead.

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
