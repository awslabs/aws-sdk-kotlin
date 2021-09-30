# Changelog

## [0.5.0-alpha] - 09/30/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes

* split auth and signing packages (#318)
    * Import paths changed to `aws.sdk.kotlin.runtime.auth.credentials` and `aws.sdk.kotlin.runtime.auth.signing`

### New features

* autofill Glacier accountId (#246)
* support JVM system property and environment variables for profiles (#297)
* expose method to sign standalone requests (#318)
* AWS configuration loader and parser (#216)

### Fixes

* utilize custom endpoint ports (#310)
* Replace junit imports with kotlin.test imports where possible (#321)
* update readme to include latest version (#319)
* sync models and endpoints (#317)
* Favor kotlin-test-juint5 over kotlin-test to resolve intermittent build failures (#316)
* kotlin 1.5.30, coroutine, kotest version bumps (#307)


## [0.4.0-alpha] - 08/26/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* ec2
* location
* marketplacecommerceanalytics

### New features

* Support for presigning requests (smithy-kotlin#435)
* Detect aws region from system properties (aws-sdk-kotlin#202)
* EC2 Query Protocol (aws-sdk-kotlin#230)
* Provide opt-in wire logging (smithy-kotlin#425)
* Support profile credentials provider (smithy-kotlin#302)

### Fixes

* s3.deleteObjects causes an exception (aws-sdk-kotlin#125)
* Streaming request BodyStream never read (aws-sdk-kotlin#282)
* location service references traits not in sdk classpath (aws-sdk-kotlin#286)
* Ignore unboxed types for subset of services (aws-sdk-kotlin#261)
* Service operations specifying no auth should not sign requests with sigv4 (aws-sdk-kotlin#263)
* Create S3 object with Unicode name fails with signature mismatch (aws-sdk-kotlin#200)
* Codegen errors in marketplacecommerceanalytics (aws-sdk-kotlin#214)
* Escape model-extra files for Windows (aws-sdk-kotlin#191)
* Support Glacier APIVersion Header (smithy-kotlin#165)
* Support APIGateway Accept Header (smithy-kotlin#157)
* Add support for awsQueryError trait (smithy-kotlin#375)
* S3 HeadObject errors require customization (aws-sdk-kotlin#152)
* S3 custom treatment of GetBucketLocation response (aws-sdk-kotlin#194)

## [0.3.0-M2] - 06/18/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* applicationcostprofiler
* apprunner
* autoscaling
* cloudformation
* cloudsearch
* cloudwatch
* docdb
* elasticache
* elasticbeanstalk
* elasticloadbalancing
* elasticloadbalancingv2
* finspace
* finspacedata
* iam
* neptune
* nimble
* rds
* redshift
* ses
* sns
* sqs
* ssmcontacts
* ssmincidents
* sts

## Changes

### New Features

* `awsQuery` protocol support (https://github.com/awslabs/smithy-kotlin/issues/127)
* detect region from environment variables (https://github.com/awslabs/smithy-kotlin/issues/356)
* custom S3 error metadata support (https://github.com/awslabs/smithy-kotlin/issues/323)
* environment credentials provider (https://github.com/awslabs/smithy-kotlin/issues/301)
* bind default HTTP client engine to CRT (https://github.com/awslabs/smithy-kotlin/issues/199)
* operation DSL overloads (https://github.com/awslabs/smithy-kotlin/issues/184)
* Kinesis read timeouts (https://github.com/awslabs/smithy-kotlin/issues/162)
* XML EOL encoding support (https://github.com/awslabs/smithy-kotlin/issues/142)

### Fixes

* path literal not escaped correctly (https://github.com/awslabs/smithy-kotlin/issues/395)
* idempotency tokens are not detected on resources (https://github.com/awslabs/smithy-kotlin/issues/376)
* httpPayload bound members need dedicated serde (https://github.com/awslabs/smithy-kotlin/issues/353)
* Route53 error messages (and maybe other restXml messages) are not deserialized and present in stacktrace
  (https://github.com/awslabs/smithy-kotlin/issues/352)
* fix url-encoding behavior of httpLabel and httpQuery members (https://github.com/awslabs/smithy-kotlin/issues/328)
* runtime error when using Kotlin 1.5.0 (https://github.com/awslabs/smithy-kotlin/issues/319)
* SES fails to build due to invalid docs (https://github.com/awslabs/aws-sdk-kotlin/issues/153)
* exception is thrown for SQS delete message (https://github.com/awslabs/aws-sdk-kotlin/issues/147)
* SNS getTopicAttributes throws an exception (https://github.com/awslabs/aws-sdk-kotlin/issues/142)
* elasticbeanstalk model generates invalid enum (https://github.com/awslabs/smithy-kotlin/issues/403)

### Other

* Kotlin 1.5.0 support
* design docs added to [docs/design](docs/design) directory

## [0.2.0-M1] - 05/10/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Known Issues

* The Kotlin SDK is not compatible with Kotlin 1.5.0.  Please use Kotlin 1.4.x.

### Services new in this release

* accessanalyzer
* acm
* acmpca
* alexaforbusiness
* amp
* amplify
* amplifybackend
* appconfig
* appflow
* appintegrations
* applicationautoscaling
* applicationdiscoveryservice
* applicationinsights
* appmesh
* appstream
* appsync
* athena
* auditmanager
* autoscalingplans
* backup
* batch
* braket
* budgets
* build
* chime
* cloud9
* clouddirectory
* cloudfront
* cloudhsm
* cloudhsmv2
* cloudsearchdomain
* cloudtrail
* cloudwatchevents
* cloudwatchlogs
* codeartifact
* codebuild
* codecommit
* codedeploy
* codeguruprofiler
* codegurureviewer
* codepipeline
* codestar
* codestarconnections
* codestarnotifications
* cognitoidentity
* cognitosync
* comprehend
* comprehendmedical
* computeoptimizer
* configservice
* connect
* connectcontactlens
* connectparticipant
* costandusagereportservice
* costexplorer
* customerprofiles
* databasemigrationservice
* databrew
* dataexchange
* datapipeline
* datasync
* dax
* detective
* devicefarm
* devopsguru
* directconnect
* directoryservice
* dlm
* dynamodbstreams
* ebs
* ec2instanceconnect
* ecr
* ecrpublic
* ecs
* efs
* eks
* elasticinference
* elasticsearchservice
* elastictranscoder
* emr
* emrcontainers
* eventbridge
* fis
* fms
* forecast
* forecastquery
* frauddetector
* fsx
* glacier
* globalaccelerator
* greengrass
* greengrassv2
* groundstation
* guardduty
* health
* healthlake
* honeycode
* identitystore
* imagebuilder
* inspector
* ivs
* kafka
* kendra
* kinesis
* kinesisanalytics
* kinesisanalyticsv2
* kinesisvideo
* kinesisvideoarchivedmedia
* kinesisvideomedia
* kinesisvideosignaling
* lakeformation
* lexmodelbuildingservice
* lexmodelsv2
* lexruntimeservice
* lexruntimev2
* licensemanager
* lookoutequipment
* lookoutmetrics
* lookoutvision
* machinelearning
* macie
* macie2
* managedblockchain
* marketplacecatalog
* marketplaceentitlementservice
* marketplacemetering
* mediaconnect
* mediapackage
* mediapackagevod
* mediastore
* mediastoredata
* mediatailor
* mgn
* migrationhub
* migrationhubconfig
* mobile
* mq
* mturk
* mwaa
* networkfirewall
* networkmanager
* opsworks
* opsworkscm
* organizations
* outposts
* personalize
* personalizeevents
* personalizeruntime
* pi
* pinpointemail
* pinpointsmsvoice
* pricing
* qldb
* qldbsession
* quicksight
* ram
* rdsdata
* redshiftdata
* rekognition
* repocache
* resourcegroups
* resourcegroupstaggingapi
* robomaker
* route53
* route53domains
* route53resolver
* s3
  * NOTE: S3 is a complicated service, this initial release **DOES NOT** have complete support for all S3 features.
* s3control
* s3outposts
* sagemaker
* sagemakera2iruntime
* sagemakeredge
* sagemakerfeaturestoreruntime
* sagemakerruntime
* savingsplans
* schemas
* serverlessapplicationrepository
* servicecatalog
* servicecatalogappregistry
* servicediscovery
* servicequotas
* sesv2
* sfn
* shield
* signer
* sms
* snowball
* sso
* ssoadmin
* ssooidc
* storagegateway
* support
* swf
* synthetics
* textract
* timestreamquery
* timestreamwrite
* transcribe
* transfer
* waf
* wafregional
* wafv2
* wellarchitected
* workdocs
* worklink
* workmail
* workmailmessageflow
* workspaces
* xray

## Changes

### New Features

* `restXml` protocol support
* add conversions to/from `java.time.Instant` and SDK `Instant` (https://github.com/awslabs/smithy-kotlin/issues/271)
* implement missing IO runtime primitives (https://github.com/awslabs/smithy-kotlin/issues/264)
* API documentation (https://github.com/awslabs/aws-sdk-kotlin/issues/119)

### Fixes

* generate per/service base exception types (https://github.com/awslabs/smithy-kotlin/issues/233)
* use sdkId if available for service client generation (https://github.com/awslabs/smithy-kotlin/issues/276)
* explicitly set jvm target compatibility (https://github.com/awslabs/aws-sdk-kotlin/issues/103)
* http error registration (https://github.com/awslabs/aws-sdk-kotlin/issues/118)
  
### Other

* generate per/service base exception types (https://github.com/awslabs/smithy-kotlin/issues/270)

## [0.1.0-M0] - 03/19/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

This is the initial beta release of AWS SDK Kotlin. It represents an early look at the overall API surface.


See the [Getting Started Guide](docs/GettingStarted.md) for how to work with beta releases and examples.


### Services in this release

* DynamoDB
* Polly
* Translate
* Cognito Identity Provider
* Secrets Manager
  * NOTE: Default idempotency token provider will not currently work, you'll need to override the config to create or update secrets until [#180](https://github.com/awslabs/smithy-kotlin/issues/180) is implemented
* KMS
* Lambda

NOTES:
* We currently can (theoretically) support any JSON based AWS protocol. If there is a service you would like to see added in a future release (before developer preview) please reach out and let us know.
* No customizations are currently implemented, some SDK's may not behave 100% correctly without such support.
* Retries, waiters, paginators, and other features are not yet implemented

### Features
* Coroutine API
* DSL Builders  
* Default (environment or config) or static credential providers only. Additional providers will be added in later releases.
* JVM only support (multiplatform support is on the roadmap)
