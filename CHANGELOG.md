# Changelog

## [1.2.46] - 07/03/2024

### Features
* (**organizations**) Added a new reason under ConstraintViolationException in RegisterDelegatedAdministrator API to prevent registering suspended accounts as delegated administrator of a service.
* (**rekognition**) This release adds support for tagging projects and datasets with the CreateProject and CreateDataset APIs.
* (**workspaces**) Fix create workspace bundle RootStorage/UserStorage to accept non null values

### Documentation
* (**applicationautoscaling**) Doc only update for Application Auto Scaling that fixes resource name.
* (**directconnect**) This update includes documentation for support of new native 400 GBps ports for Direct Connect.

## [1.2.45] - 07/02/2024

### Features
* (**ec2**) Documentation updates for Elastic Compute Cloud (EC2).
* (**fms**) Increases Customer API's ManagedServiceData length
* (**s3**) Added response overrides to Head Object requests.

## [1.2.44] - 07/01/2024

### Features
* (**apigateway**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**cognitoidentity**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**connect**) Authentication profiles are Amazon Connect resources (in gated preview) that allow you to configure authentication settings for users in your contact center. This release adds support for new ListAuthenticationProfiles, DescribeAuthenticationProfile and UpdateAuthenticationProfile APIs.
* (**docdb**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**eks**) Updates EKS managed node groups to support EC2 Capacity Blocks for ML
* (**paymentcryptography**) Added further restrictions on logging of potentially sensitive inputs and outputs.
* (**paymentcryptographydata**) Adding support for dynamic keys for encrypt, decrypt, re-encrypt and translate pin functions.  With this change, customers can use one-time TR-31 keys directly in dataplane operations without the need to first import them into the service.
* (**sfn**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**swf**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

### Documentation
* (**wafv2**) JSON body inspection: Update documentation to clarify that JSON parsing doesn't include full validation.

### Miscellaneous
* Upgrade to ktlint v1.3.0
* Upgrade to smithy-kotlin v1.2.11

## [1.2.43] - 06/28/2024

### Features
* (**acmpca**) Added CCPC_LEVEL_1_OR_HIGHER KeyStorageSecurityStandard and SM2 KeyAlgorithm and SM3WITHSM2 SigningAlgorithm for China regions.
* (**cloudhsmv2**) Added 3 new APIs to support backup sharing: GetResourcePolicy, PutResourcePolicy, and DeleteResourcePolicy. Added BackupArn to the output of the DescribeBackups API. Added support for BackupArn in the CreateCluster API.
* (**connect**) This release supports showing PreferredAgentRouting step via DescribeContact API.
* (**emr**) This release provides the support for new allocation strategies i.e. CAPACITY_OPTIMIZED_PRIORITIZED for Spot and PRIORITIZED for On-Demand by taking input of priority value for each instance type for instance fleet clusters.
* (**glue**) Added AttributesToGet parameter to Glue GetDatabases, allowing caller to limit output to include only the database name.
* (**kinesisanalyticsv2**) Support for Flink 1.19 in Managed Service for Apache Flink
* (**opensearch**) This release removes support for enabling or disabling Natural Language Query Processing feature for Amazon OpenSearch Service domains.
* (**workspaces**) Added support for Red Hat Enterprise Linux 8 on Amazon WorkSpaces Personal.

### Documentation
* (**pi**) Noting that the filter db.sql.db_id isn't available for RDS for SQL Server DB instances.

## [1.2.42] - 06/27/2024

### Features
* (**applicationautoscaling**) Amazon WorkSpaces customers can now use Application Auto Scaling to automatically scale the number of virtual desktops in a WorkSpaces pool.
* (**chimesdkmediapipelines**) Added Amazon Transcribe multi language identification to Chime SDK call analytics. Enabling customers sending single stream audio to generate call recordings using Chime SDK call analytics
* (**datazone**) This release supports the data lineage feature of business data catalog in Amazon DataZone.
* (**elasticache**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**mq**) This release makes the EngineVersion field optional for both broker and configuration and uses the latest available version by default. The AutoMinorVersionUpgrade field is also now optional for broker creation and defaults to 'true'.
* (**qconnect**) Adds CreateContentAssociation, ListContentAssociations, GetContentAssociation, and DeleteContentAssociation APIs.
* (**quicksight**) Adding support for Repeating Sections, Nested Filters
* (**sagemaker**) Add capability for Admins to customize Studio experience for the user by showing or hiding Apps and MLTools.
* (**workspaces**) Added support for WorkSpaces Pools.

### Documentation
* (**cloudfront**) Doc only update for CloudFront that fixes customer-reported issue
* (**rds**) Updates Amazon RDS documentation for TAZ export to S3.

## [1.2.41] - 06/26/2024

### Features
* (**controltower**) Added ListLandingZoneOperations API.
* (**eks**) Added support for disabling unmanaged addons during cluster creation.
* (**ivsrealtime**) IVS Real-Time now offers customers the ability to upload public keys for customer vended participant tokens.
* (**kinesisanalyticsv2**) This release adds support for new ListApplicationOperations and DescribeApplicationOperation APIs. It adds a new configuration to enable system rollbacks, adds field ApplicationVersionCreateTimestamp for clarity and improves support for pagination for APIs.
* (**opensearch**) This release adds support for enabling or disabling Natural Language Query Processing feature for Amazon OpenSearch Service domains, and provides visibility into the current state of the setup or tear-down.

## [1.2.40] - 06/25/2024

### Features
* (**autoscaling**) Doc only update for Auto Scaling's TargetTrackingMetricDataQuery
* (**ec2**) This release is for the launch of the new u7ib-12tb.224xlarge, R8g, c7gn.metal and mac2-m1ultra.metal instance types
* (**networkmanager**) This is model changes & documentation update for the Asynchronous Error Reporting feature for AWS Cloud WAN. This feature allows customers to view errors that occur while their resources are being provisioned, enabling customers to fix their resources without needing external support.
* (**workspacesthinclient**) This release adds the deviceCreationTags field to CreateEnvironment API input, UpdateEnvironment API input and GetEnvironment API output.

## [1.2.39] - 06/24/2024

### Features
* (**bedrockruntime**) Increases Converse API's document name length
* (**customerprofiles**) This release includes changes to ProfileObjectType APIs, adds functionality top set and get capacity for profile object types.
* (**ec2**) Fix EC2 multi-protocol info in models.
* (**qbusiness**) Allow enable/disable Q Apps when creating/updating a Q application; Return the Q Apps enablement information when getting a Q application.
* (**ssm**) Add sensitive trait to SSM IPAddress property for CloudTrail redaction
* (**workspacesweb**) Added ability to enable DeepLinking functionality on a Portal via UserSettings as well as added support for IdentityProvider resource tagging.

## [1.2.38] - 06/20/2024

### Features
* (**bedrockruntime**) This release adds document support to Converse and ConverseStream APIs
* (**codeartifact**) Add support for the Cargo package format.
* (**computeoptimizer**) This release enables AWS Compute Optimizer to analyze and generate optimization recommendations for Amazon RDS MySQL and RDS PostgreSQL.
* (**costoptimizationhub**) This release enables AWS Cost Optimization Hub to show cost optimization recommendations for Amazon RDS MySQL and RDS PostgreSQL.
* (**glue**) Fix Glue paginators for Jobs, JobRuns, Triggers, Blueprints and Workflows.
* (**ivsrealtime**) IVS Real-Time now offers customers the ability to record individual stage participants to S3.
* (**sagemaker**) Adds support for model references in Hub service, and adds support for cross-account access of Hubs

### Documentation
* (**dynamodb**) Doc-only update for DynamoDB. Fixed Important note in 6 Global table APIs - CreateGlobalTable, DescribeGlobalTable, DescribeGlobalTableSettings, ListGlobalTables, UpdateGlobalTable, and UpdateGlobalTableSettings.
* (**securityhub**) Documentation updates for Security Hub

## [1.2.37] - 06/19/2024

### Features
* (**artifact**) This release adds an acceptanceType field to the ReportSummary structure (used in the ListReports API response).
* (**athena**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**costandusagereportservice**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**directconnect**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**elastictranscoder**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**opensearch**) This release enables customers to use JSON Web Tokens (JWT) for authentication on their Amazon OpenSearch Service domains.

## [1.2.36] - 06/18/2024

### Features
* (**bedrockruntime**) This release adds support for using Guardrails with the Converse and ConverseStream APIs.
* (**cloudtrail**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**configservice**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**eks**) This release adds support to surface async fargate customer errors from async path to customer through describe-fargate-profile API response.
* (**lightsail**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**polly**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**rekognition**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**sagemaker**) Launched a new feature in SageMaker to provide managed MLflow Tracking Servers for customers to track ML experiments. This release also adds a new capability of attaching additional storage to SageMaker HyperPod cluster instances.
* (**shield**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**snowball**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## [1.2.35] - 06/17/2024

### Features
* (**batch**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**codebuild**) AWS CodeBuild now supports global and organization GitHub webhooks
* (**cognitoidentityprovider**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**directoryservice**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**efs**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**glue**) This release introduces a new feature, Usage profiles. Usage profiles allow the AWS Glue admin to create different profiles for various classes of users within the account, enforcing limits and defaults for jobs and sessions.
* (**mediaconvert**) This release includes support for creating I-frame only video segments for DASH trick play.
* (**waf**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

### Documentation
* (**acmpca**) Doc-only update that adds name constraints as an allowed extension for ImportCertificateAuthorityCertificate.
* (**kms**) Updating SDK example for KMS DeriveSharedSecret API.
* (**secretsmanager**) Doc only update for Secrets Manager

## [1.2.34] - 06/14/2024

### Features
* (**datazone**) This release introduces a new default service blueprint for custom environment creation.
* (**macie2**) This release adds support for managing the status of automated sensitive data discovery for individual accounts in an organization, and determining whether individual S3 buckets are included in the scope of the analyses.
* (**mediaconvert**) This release adds the ability to search for historical job records within the management console using a search box and/or via the SDK/CLI with partial string matching search on input file name.
* (**route53domains**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

### Documentation
* (**ec2**) Documentation updates for Amazon EC2.

### Miscellaneous
* Upgrade to Smithy 1.49.0

## [1.2.33] - 06/13/2024

### Features
* (**cloudhsmv2**) Added support for hsm type hsm2m.medium. Added supported for creating a cluster in FIPS or NON_FIPS mode.
* (**glue**) This release adds support for configuration of evaluation method for composite rules in Glue Data Quality rulesets.
* (**iotwireless**) Add RoamingDeviceSNR and RoamingDeviceRSSI to Customer Metrics.
* (**kms**) This feature allows customers to use their keys stored in KMS to derive a shared secret which can then be used to establish a secured channel for communication, provide proof of possession, or establish trust with other parties.
* (**mediapackagev2**) This release adds support for CMAF ingest (DASH-IF live media ingest protocol interface 1)

## [1.2.32] - 06/12/2024

### Features
* (**apptest**) AWS Mainframe Modernization Application Testing is an AWS Mainframe Modernization service feature that automates functional equivalence testing for mainframe application modernization and migration to AWS, and regression testing.
* (**ec2**) Tagging support for Traffic Mirroring FilterRule resource
* (**osis**) SDK changes for self-managed vpc endpoint to OpenSearch ingestion pipelines.
* (**secretsmanager**) Introducing RotationToken parameter for PutSecretValue API
* (**securitylake**) This release updates request validation regex to account for non-commercial aws partitions.
* (**sesv2**) This release adds support for Amazon EventBridge as an email sending events destination.

### Documentation
* (**redshift**) Updates to remove DC1 and DS2 node types.

### Miscellaneous
* Deprecation of AWS Backup Storage

## [1.2.31] - 06/11/2024

### Features
* (**accessanalyzer**) IAM Access Analyzer now provides policy recommendations to help resolve unused permissions for IAM roles and users. Additionally, IAM Access Analyzer now extends its custom policy checks to detect when IAM policies grant public access or access to critical resources ahead of deployments.
* (**guardduty**) Added API support for GuardDuty Malware Protection for S3.
* (**networkmanager**) This is model changes & documentation update for Service Insertion feature for AWS Cloud WAN. This feature allows insertion of AWS/3rd party security services on Cloud WAN. This allows to steer inter/intra segment traffic via security appliances and provide visibility to the route updates.
* (**pcaconnectorscep**) Connector for SCEP allows you to use a managed, cloud CA to enroll mobile devices and networking gear. SCEP is a widely-adopted protocol used by mobile device management (MDM) solutions for enrolling mobile devices. With the connector, you can use AWS Private CA with popular MDM solutions.
* (**sagemaker**) Introduced Scope and AuthenticationRequestExtraParams to SageMaker Workforce OIDC configuration; this allows customers to modify these options for their private Workforce IdP integration. Model Registry Cross-account model package groups are discoverable.

## [1.2.30] - 06/10/2024

### Features
* (**applicationsignals**) This is the initial SDK release for Amazon CloudWatch Application Signals. Amazon CloudWatch Application Signals provides curated application performance monitoring for developers to monitor and troubleshoot application health using pre-built dashboards and Service Level Objectives.
* (**ecs**) This release introduces a new cluster configuration to support the customer-managed keys for ECS managed storage encryption.
* (**imagebuilder**) This release updates the regex pattern for Image Builder ARNs.

## [1.2.29] - 06/07/2024

### Features
* (**auditmanager**) New feature: common controls. When creating custom controls, you can now use pre-grouped AWS data sources based on common compliance themes. Also, the awsServices parameter is deprecated because we now manage services in scope for you. If used, the input is ignored and an empty list is returned.
* (**b2bi**) Added exceptions to B2Bi List operations and ConflictException to B2Bi StartTransformerJob operation. Also made capabilities field explicitly required when creating a Partnership.
* (**codepipeline**) CodePipeline now supports overriding S3 Source Object Key during StartPipelineExecution, as part of Source Overrides.
* (**sagemaker**) This release introduces a new optional parameter: InferenceAmiVersion, in ProductionVariant.
* (**verifiedpermissions**) This release adds OpenIdConnect (OIDC) configuration support for IdentitySources, allowing for external IDPs to be used in authorization requests.

## [1.2.28] - 06/06/2024

### Features
* (**account**) This release adds 3 new APIs (AcceptPrimaryEmailUpdate, GetPrimaryEmail, and StartPrimaryEmailUpdate) used to centrally manage the root user email address of member accounts within an AWS organization.
* (**firehose**) Adds integration with Secrets Manager for Redshift, Splunk, HttpEndpoint, and Snowflake destinations
* (**fsx**) This release adds support to increase metadata performance on FSx for Lustre file systems beyond the default level provisioned when a file system is created. This can be done by specifying MetadataConfiguration during the creation of Persistent_2 file systems or by updating it on demand.
* (**glue**) This release adds support for creating and updating Glue Data Catalog Views.
* (**iotwireless**) Adds support for wireless device to be in Conflict FUOTA Device Status due to a FUOTA Task, so it couldn't be attached to a new one.
* (**location**) Added two new APIs, VerifyDevicePosition and ForecastGeofenceEvents. Added support for putting larger geofences up to 100,000 vertices with Geobuf fields.
* (**storagegateway**) Adds SoftwareUpdatePreferences to DescribeMaintenanceStartTime and UpdateMaintenanceStartTime, a structure which contains AutomaticUpdatePolicy.

### Documentation
* (**sns**) Doc-only update for SNS. These changes include customer-reported issues and TXC3 updates.
* (**sqs**) Doc only updates for SQS. These updates include customer-reported issues and TCX3 modifications.

## [1.2.27] - 06/05/2024

### Features
* (**globalaccelerator**) This release contains a new optional ip-addresses input field for the update accelerator and update custom routing accelerator apis. This input enables consumers to replace IPv4 addresses on existing accelerators with addresses provided in the input.
* (**glue**) AWS Glue now supports native SaaS connectivity: Salesforce connector available now
* (**s3**) Added new params copySource and key to copyObject API for supporting S3 Access Grants plugin. These changes will not change any of the existing S3 API functionality.

## [1.2.26] - 06/04/2024

### Features
* (**ec2**) U7i instances with up to 32 TiB of DDR5 memory and 896 vCPUs are now available. C7i-flex instances are launched and are lower-priced variants of the Amazon EC2 C7i instances that offer a baseline level of CPU performance with the ability to scale up to the full compute performance 95% of the time.
* (**pipes**) This release adds Timestream for LiveAnalytics as a supported target in EventBridge Pipes
* (**sagemaker**) Extend DescribeClusterNode response with private DNS hostname and IP address, and placement information about availability zone and availability zone ID.
* (**taxsettings**) Initial release of AWS Tax Settings API

## [1.2.25] - 06/03/2024

### Features
* (**batch**) This release adds support for the AWS Batch GetJobQueueSnapshot API operation.
* (**eks**) Adds support for EKS add-ons pod identity associations integration
* (**iottwinmaker**) Support RESET_VALUE UpdateType for PropertyUpdates to reset property value to default or null

### Documentation
* (**amplify**) This doc-only update identifies fields that are specific to Gen 1 and Gen 2 applications.

## [1.2.24] - 05/31/2024

### Features
* (**codegurusecurity**) This release includes minor model updates and documentation updates.
* (**launchwizard**) This release adds support for describing workload deployment specifications, deploying additional workload types, and managing tags for Launch Wizard resources with API operations.

### Fixes
* [#1315](https://github.com/awslabs/aws-sdk-kotlin/issues/1315) Disable proxying of requests made to EC2 IMDS

### Documentation
* (**codebuild**) AWS CodeBuild now supports Self-hosted GitHub Actions runners for Github Enterprise
* (**elasticache**) Update to attributes of TestFailover and minor revisions.

### Miscellaneous
* [#1303](https://github.com/awslabs/aws-sdk-kotlin/issues/1303) Add trailing slash to base IMDS endpoint

## [1.2.23] - 05/30/2024

### Features
* (**acm**) add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**bedrockagent**) With this release, Knowledge bases for Bedrock adds support for Titan Text Embedding v2.
* (**bedrockruntime**) This release adds Converse and ConverseStream APIs to Bedrock Runtime
* (**cloudtrail**) CloudTrail Lake returns PartitionKeys in the GetEventDataStore API response. Events are grouped into partitions based on these keys for better query performance. For example, the calendarday key groups events by day, while combining the calendarday key with the hour key groups them by day and hour.
* (**connect**) Adding associatedQueueIds as a SearchCriteria and response field to the SearchRoutingProfiles API
* (**emrserverless**) The release adds support for spark structured streaming.
* (**sagemaker**) Adds Model Card information as a new component to Model Package. Autopilot launches algorithm selection for TimeSeries modality to generate AutoML candidates per algorithm.

### Documentation
* (**rds**) Updates Amazon RDS documentation for Aurora Postgres DBname.

## [1.2.22] - 05/29/2024

### Features
* (**athena**) Throwing validation errors on CreateNotebook with Name containing `/`,`:`,`\`
* (**codebuild**) AWS CodeBuild now supports manually creating GitHub webhooks
* (**connect**) This release includes changes to DescribeContact API's response by including ConnectedToSystemTimestamp, RoutingCriteria, Customer, Campaign, AnsweringMachineDetectionStatus, CustomerVoiceActivity, QualityMetrics, DisconnectDetails, and SegmentAttributes information from a contact in Amazon Connect.
* (**glue**) Add optional field JobMode to CreateJob and UpdateJob APIs.
* (**securityhub**) Add ROOT type for TargetType model

## [1.2.21] - 05/28/2024

### Features
* (**ec2**) Providing support to accept BgpAsnExtended attribute
* (**kafka**) Adds ControllerNodeInfo in ListNodes response to support Raft mode for MSK
* (**swf**) This release adds new APIs for deleting activity type and workflow type resources.

### Documentation
* (**dynamodb**) Doc-only update for DynamoDB. Specified the IAM actions needed to authorize a user to create a table with a resource-based policy.

## [1.2.20] - 05/24/2024

### Features
* (**iotfleetwise**) AWS IoT FleetWise now supports listing vehicles with attributes filter, ListVehicles API is updated to support additional attributes filter.

### Documentation
* (**dynamodb**) Documentation only updates for DynamoDB.
* (**managedblockchain**) This is a minor documentation update to address the impact of the shut down of the Goerli and Polygon networks.

## [1.2.19] - 05/23/2024

### Features
* (**emrserverless**) This release adds the capability to run interactive workloads using Apache Livy Endpoint.

### Documentation
* (**opsworks**) Documentation-only update for OpsWorks Stacks.

## [1.2.18] - 05/22/2024

### Features
* (**chatbot**) This change adds support for tagging Chatbot configurations.
* (**cloudformation**) Added DeletionMode FORCE_DELETE_STACK for deleting a stack that is stuck in DELETE_FAILED state due to resource deletion failure.
* (**kms**) This release includes feature to import customer's asymmetric (RSA, ECC and SM2) and HMAC keys into KMS in China.
* (**opensearch**) This release adds support for enabling or disabling a data source configured as part of Zero-ETL integration with Amazon S3, by setting its status.
* (**wafv2**) You can now use Security Lake to collect web ACL traffic data.

## [1.2.17] - 05/21/2024

### Features
* (**cloudfront**) Model update; no change to SDK functionality.
* (**glue**) Add Maintenance window to CreateJob and UpdateJob APIs and JobRun response. Add a new Job Run State for EXPIRED.
* (**lightsail**) This release adds support for Amazon Lightsail instances to switch between dual-stack or IPv4 only and IPv6-only public IP address types.
* (**mailmanager**) This release includes a new Amazon SES feature called Mail Manager, which is a set of email gateway capabilities designed to help customers strengthen their organization's email infrastructure, simplify email workflow management, and streamline email compliance control.
* (**pi**) Performance Insights added a new input parameter called AuthorizedActions to support the fine-grained access feature. Performance Insights also restricted the acceptable input characters.
* (**storagegateway**) Added new SMBSecurityStrategy enum named MandatoryEncryptionNoAes128, new mode enforces encryption and disables AES 128-bit algorithums.

### Documentation
* (**rds**) Updates Amazon RDS documentation for Db2 license through AWS Marketplace.

## [1.2.16] - 05/20/2024

### Features
* (**bedrockagent**) This release adds support for using Guardrails with Bedrock Agents.
* (**bedrockagentruntime**) This release adds support for using Guardrails with Bedrock Agents.
* (**controltower**) Added ListControlOperations API and filtering support for ListEnabledControls API. Updates also includes added metadata for enabled controls and control operations.
* (**osis**) Add support for creating an OpenSearch Ingestion pipeline that is attached to a provided VPC. Add information about the destinations of an OpenSearch Ingestion pipeline to the GetPipeline and ListPipelines APIs.
* (**rds**) This release adds support for EngineLifecycleSupport on DBInstances, DBClusters, and GlobalClusters.
* (**secretsmanager**) add v2 smoke tests and smithy smokeTests trait for SDK testing

## [1.2.15] - 05/17/2024

### Features
* (**applicationautoscaling**) add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**codebuild**) Aws CodeBuild now supports 36 hours build timeout
* (**elasticloadbalancingv2**) This release adds dualstack-without-public-ipv4 IP address type for ALB.
* (**lakeformation**) Introduces a new API, GetDataLakePrincipal, that returns the identity of the invoking principal
* (**transfer**) Enable use of CloudFormation traits in Smithy model to improve generated CloudFormation schema from the Smithy API model.

### Miscellaneous
* [#1092](https://github.com/smithy-lang/smithy-kotlin/issues/1092) Upgrade to version [**1.2.4**](https://github.com/smithy-lang/smithy-kotlin/releases/tag/v1.2.4) of **smithy-kotlin** to pick up fix for `http.nonProxyHosts` wildcards

## [1.2.14] - 05/16/2024

### Features
* (**acmpca**) This release adds support for waiters to fail on AccessDeniedException when having insufficient permissions
* (**kafka**) AWS MSK support for Broker Removal.
* (**mwaa**) Amazon MWAA now supports Airflow web server auto scaling to automatically handle increased demand from REST APIs, Command Line Interface (CLI), or more Airflow User Interface (UI) users. Customers can specify maximum and minimum web server instances during environment creation and update workflow.
* (**quicksight**) This release adds DescribeKeyRegistration and UpdateKeyRegistration APIs to manage QuickSight Customer Managed Keys (CMK).
* (**sagemaker**) Introduced WorkerAccessConfiguration to SageMaker Workteam. This allows customers to configure resource access for workers in a workteam.

### Documentation
* (**connect**) Adding Contact Flow metrics to the GetMetricDataV2 API
* (**secretsmanager**) Documentation updates for AWS Secrets Manager

## [1.2.13] - 05/15/2024

### Features
* (**bedrockagentruntime**) Updating Bedrock Knowledge Base Metadata & Filters feature with two new filters listContains and stringContains
* (**codebuild**) CodeBuild Reserved Capacity VPC Support
* (**datasync**) Task executions now display a CANCELLING status when an execution is in the process of being cancelled.
* (**grafana**) This release adds new ServiceAccount and ServiceAccountToken APIs.
* (**medicalimaging**) Added support for importing medical imaging data from Amazon S3 buckets across accounts and regions.

### Documentation
* (**securityhub**) Documentation-only update for AWS Security Hub

## [1.2.12] - 05/14/2024

### Features
* (**connect**) Amazon Connect provides enhanced search capabilities for flows & flow modules on the Connect admin website and programmatically using APIs. You can search for flows and flow modules by name, description, type, status, and tags, to filter and identify a specific flow in your Connect instances.
* (**s3**) Updated a few x-id in the http uri traits

### Miscellaneous
* Remove Alexa for Business service
* Remove Honeycode service

## [1.2.11] - 05/13/2024

### Features
* (**eventbridge**) Amazon EventBridge introduces KMS customer-managed key (CMK) encryption support for custom and partner events published on EventBridge Event Bus (including default bus) and UpdateEventBus API.
* (**vpclattice**) This release adds TLS Passthrough support. It also increases max number of target group per rule to 10.

## [1.2.10] - 05/10/2024

### Features
* (**applicationdiscoveryservice**) add v2 smoke tests and smithy smokeTests trait for SDK testing
* (**greengrassv2**) Mark ComponentVersion in ComponentDeploymentSpecification as required.
* (**sagemaker**) Introduced support for G6 instance types on Sagemaker Notebook Instances and on SageMaker Studio for JupyterLab and CodeEditor applications.
* (**ssooidc**) Updated request parameters for PKCE support.

## [1.2.9] - 05/09/2024

### Features
* (**bedrockagentruntime**) This release adds support to provide guardrail configuration and modify inference parameters that are then used in RetrieveAndGenerate API in Agents for Amazon Bedrock.
* (**pinpoint**) This release adds support for specifying email message headers for Email Templates, Campaigns, Journeys and Send Messages.
* (**ssmsap**) Added support for application-aware start/stop of SAP applications running on EC2 instances, with SSM for SAP
* (**verifiedpermissions**) Adds policy effect and actions fields to Policy API's.

### Documentation
* (**route53resolver**) Update the DNS Firewall settings to correct a spelling issue.

## [1.2.8] - 05/08/2024

### Features
* (**cognitoidentityprovider**) Add EXTERNAL_PROVIDER enum value to UserStatusType.
* (**ec2**) Adding Precision Hardware Clock (PHC) to public API DescribeInstanceTypes
* (**ecr**) This release adds pull through cache rules support for GitLab container registry in Amazon ECR.
* (**polly**) Add new engine - generative - that builds the most expressive conversational voices.
* (**sqs**) This release adds MessageSystemAttributeNames to ReceiveMessageRequest to replace AttributeNames.

### Documentation
* (**fms**) The policy scope resource tag is always a string value, either a non-empty string or an empty string.

## [1.2.7] - 05/07/2024

### Features
* (**budgets**) This release adds tag support for budgets and budget actions.
* (**resiliencehub**) AWS Resilience Hub has expanded its drift detection capabilities by introducing a new type of drift detection - application resource drift. This new enhancement detects changes, such as the addition or deletion of resources within the application's input sources.

### Documentation
* (**b2bi**) Documentation update to clarify the MappingTemplate definition.
* (**route53profiles**) Doc only update for Route 53 profiles that fixes some link  issues

## [1.2.6] - 05/06/2024

### Features
* (**medialive**) AWS Elemental MediaLive now supports configuring how SCTE 35 passthrough triggers segment breaks in HLS and MediaPackage output groups. Previously, messages triggered breaks in all these output groups. The new option is to trigger segment breaks only in groups that have SCTE 35 passthrough enabled.

## [1.2.5] - 05/03/2024

### Features
* (**bedrockagent**) This release adds support for using Provisioned Throughput with Bedrock Agents.
* (**connect**) This release adds 5 new APIs for managing attachments: StartAttachedFileUpload, CompleteAttachedFileUpload, GetAttachedFile, BatchGetAttachedFileMetadata, DeleteAttachedFile. These APIs can be used to programmatically upload and download attachments to Connect resources, like cases.
* (**connectcases**) This feature supports the release of Files related items
* (**inspector2**) This release adds CSV format to GetCisScanReport for Inspector v2
* (**sagemaker**) Amazon SageMaker Inference now supports m6i, c6i, r6i, m7i, c7i, r7i and g5 instance types for Batch Transform Jobs
* (**sesv2**) Adds support for specifying replacement headers per BulkEmailEntry in SendBulkEmail in SESv2.

### Documentation
* (**datasync**) Updated guidance on using private or self-signed certificate authorities (CAs) with AWS DataSync object storage locations.

## [1.2.4] - 05/02/2024

### Features
* (**dynamodb**) This release adds support to specify an optional, maximum OnDemandThroughput for DynamoDB tables and global secondary indexes in the CreateTable or UpdateTable APIs. You can also override the OnDemandThroughput settings by calling the ImportTable, RestoreFromPointInTime, or RestoreFromBackup APIs.
* (**ec2**) This release includes a new API for retrieving the public endorsement key of the EC2 instance's Nitro Trusted Platform Module (NitroTPM).
* (**personalize**) This releases ability to delete users and their data, including their metadata and interactions data, from a dataset group.
* (**redshiftserverless**) Update Redshift Serverless List Scheduled Actions Output Response to include Namespace Name.

## [1.2.3] - 05/01/2024

### Features
* (**bedrockagent**) This release adds support for using MongoDB Atlas as a vector store when creating a knowledge base.
* (**personalizeruntime**) This release adds support for a Reason attribute for predicted items generated by User-Personalization-v2.
* (**securityhub**) Updated CreateMembers API request with limits.
* (**sesv2**) Fixes ListContacts and ListImportJobs APIs to use POST instead of GET.

### Documentation
* (**ec2**) Documentation updates for Amazon EC2.

## [1.2.2] - 04/30/2024

### Features
* (**chimesdkvoice**) Due to changes made by the Amazon Alexa service, GetSipMediaApplicationAlexaSkillConfiguration and PutSipMediaApplicationAlexaSkillConfiguration APIs are no longer available for use. For more information, refer to the Alexa Smart Properties page.
* (**codeartifact**) Add support for the Ruby package format.
* (**fms**) AWS Firewall Manager now supports the network firewall service stream exception policy feature for accounts within your organization.
* (**omics**) Add support for workflow sharing and dynamic run storage
* (**opensearch**) This release enables customers to create Route53 A and AAAA alias record types to point custom endpoint domain to OpenSearch domain's dualstack search endpoint.
* (**pinpointsmsvoicev2**) Amazon Pinpoint has added two new features Multimedia services (MMS) and protect configurations. Use the three new MMS APIs to send media messages to a mobile phone which includes image, audio, text, or video files. Use the ten new protect configurations APIs to block messages to specific countries.
* (**qbusiness**) This is a general availability (GA) release of Amazon Q Business. Q Business enables employees in an enterprise to get comprehensive answers to complex questions and take actions through a unified, intuitive web-based chat experience - using an enterprise's existing content, data, and systems.
* (**quicksight**) New Q embedding supporting Generative Q&A
* (**route53resolver**) Release of FirewallDomainRedirectionAction parameter on the Route 53 DNS Firewall Rule.  This allows customers to configure a DNS Firewall rule to inspect all the domains in the DNS redirection chain (default) , such as CNAME, ALIAS, DNAME, etc., or just the first domain and trust the rest.
* (**sagemaker**) Amazon SageMaker Training now supports the use of attribute-based access control (ABAC) roles for training job execution roles. Amazon SageMaker Inference now supports G6 instance types.

### Documentation
* (**signer**) Documentation updates for AWS Signer. Adds cross-account signing constraint and definitions for cross-account actions.

## [1.2.1] - 04/29/2024

### Features
* (**amplify**) Updating max results limit for listing any resources (Job, Artifacts, Branch, BackendResources, DomainAssociation) to 50 with the exception of list apps that where max results can be up to 100.
* (**connectcases**) This feature releases DeleteField, DeletedLayout, and DeleteTemplate API's
* (**inspector2**) Update Inspector2 to include new Agentless API parameters.
* (**timestreamquery**) This change allows users to update and describe account settings associated with their accounts.
* (**transcribe**) This update provides error messaging for generative call summarization in Transcribe Call Analytics
* (**trustedadvisor**) This release adds the BatchUpdateRecommendationResourceExclusion API to support batch updates of Recommendation Resource exclusion statuses and introduces a new exclusion status filter to the ListRecommendationResources and ListOrganizationRecommendationResources APIs.

## [1.2.0] - 04/27/2024

### Fixes
* [#1211](https://github.com/awslabs/aws-sdk-kotlin/issues/1211) ⚠️ **IMPORTANT**: Apply clock skew interceptor to clients created via `invoke`

### Miscellaneous
* ⚠️ **IMPORTANT**: Upgrade to latest versions of OkHttp, Okio, Kotlin

## [1.1.27] - 04/26/2024

### Features
* (**codepipeline**) Add ability to manually and automatically roll back a pipeline stage to a previously successful execution.
* (**cognitoidentityprovider**) Add LimitExceededException to SignUp errors
* (**connectcampaigns**) This release adds support for specifying if Answering Machine should wait for prompt sound.
* (**marketplaceentitlementservice**) Releasing minor endpoint updates.
* (**oam**) This release introduces support for Source Accounts to define which Metrics and Logs to share with the Monitoring Account
* (**rds**) SupportsLimitlessDatabase field added to describe-db-engine-versions to indicate whether the DB engine version supports Aurora Limitless Database.
* (**support**) Releasing minor endpoint updates.

## [1.1.26] - 04/25/2024

### Features
* (**appsync**) UpdateGraphQLAPI documentation update and datasource introspection secret arn update
* (**fms**) AWS Firewall Manager adds support for network ACL policies to manage Amazon Virtual Private Cloud (VPC) network access control lists (ACLs) for accounts in your organization.
* (**ivs**) Bug Fix: IVS does not support arns with the `svs` prefix
* (**ivsrealtime**) Bug Fix: IVS Real Time does not support ARNs using the `svs` prefix.
* (**sfn**) Add new ValidateStateMachineDefinition operation, which performs syntax checking on the definition of a Amazon States Language (ASL) state machine.

### Documentation
* (**rds**) Updates Amazon RDS documentation for setting local time zones for RDS for Db2 DB instances.

## [1.1.25] - 04/24/2024

### Features
* (**datasync**) This change allows users to disable and enable the schedules associated with their tasks.
* (**ec2**) Launching capability for customers to enable or disable automatic assignment of public IPv4 addresses to their network interface
* (**emrcontainers**) EMRonEKS Service support for SecurityConfiguration enforcement for Spark Jobs.
* (**entityresolution**) Support Batch Unique IDs Deletion.
* (**gamelift**) Amazon GameLift releases container fleets support for public preview. Deploy Linux-based containerized game server software for hosting on Amazon GameLift.
* (**ssm**) Add SSM DescribeInstanceProperties API to public AWS SDK.

## [1.1.24] - 04/23/2024

### Features
* (**bedrock**) This release introduces Model Evaluation and Guardrails for Amazon Bedrock.
* (**bedrockagent**) Introducing the ability to create multiple data sources per knowledge base, specify S3 buckets as data sources from external accounts, and exposing levers to define the deletion behavior of the underlying vector store data.
* (**bedrockagentruntime**) This release introduces zero-setup file upload support for the RetrieveAndGenerate API. This allows you to chat with your data without setting up a Knowledge Base.
* (**bedrockruntime**) This release introduces Guardrails for Amazon Bedrock.
* (**costexplorer**) Added additional metadata that might be applicable to your reservation recommendations.
* (**ec2**) This release introduces EC2 AMI Deregistration Protection, a new AMI property that can be enabled by customers to protect an AMI against an unintended deregistration. This release also enables the AMI owners to view the AMI 'LastLaunchedTime' in DescribeImages API.
* (**workspacesweb**) Added InstanceType and MaxConcurrentSessions parameters on CreatePortal and UpdatePortal Operations as well as the ability to read Customer Managed Key & Additional Encryption Context parameters on supported resources (Portal, BrowserSettings, UserSettings, IPAccessSettings)

### Documentation
* (**pi**) Clarifies how aggregation works for GetResourceMetrics in the Performance Insights API.
* (**rds**) Fix the example ARN for ModifyActivityStreamRequest

## [1.1.23] - 04/22/2024

### Features
* (**bedrockagent**) Releasing the support for simplified configuration and return of control
* (**bedrockagentruntime**) Releasing the support for simplified configuration and return of control
* (**paymentcryptography**) Adding support to TR-31/TR-34 exports for optional headers, allowing customers to add additional metadata (such as key version and KSN) when exporting keys from the service.
* (**route53profiles**) Route 53 Profiles allows you to apply a central DNS configuration across many VPCs regardless of account.
* (**sagemaker**) This release adds support for Real-Time Collaboration and Shared Space for JupyterLab App on SageMaker Studio.
* (**transfer**) Adding new API to support remote directory listing using SFTP connector

### Fixes
* [#900](https://github.com/awslabs/aws-sdk-kotlin/issues/900) Correctly generate presigners, waiters, and paginators for resource operations

### Documentation
* (**redshiftserverless**) Updates description of schedule field for scheduled actions.
* (**servicediscovery**) This release adds examples to several Cloud Map actions.

## [1.1.22] - 04/19/2024

### Features
* (**glue**) Adding RowFilter in the response for GetUnfilteredTableMetadata API
* (**internetmonitor**) This update introduces the GetInternetEvent and ListInternetEvents APIs, which provide access to internet events displayed on the Amazon CloudWatch Internet Weather Map.
* (**personalize**) This releases auto training capability while creating a solution and automatically syncing latest solution versions when creating/updating a campaign
* Customize S3's `Expires` field, including adding a new `ExpiresString` field for output types.

### Fixes
* [#1210](https://github.com/awslabs/aws-sdk-kotlin/issues/1210) Service client based identity providers now respect AWS_ENDPOINT_URL_SERVICE environment variables

## [1.1.21] - 04/18/2024

### Features
* (**drs**) Outpost ARN added to Source Server and Recovery Instance
* (**emrserverless**) This release adds the capability to publish detailed Spark engine metrics to Amazon Managed Service for Prometheus (AMP) for  enhanced monitoring for Spark jobs.
* (**guardduty**) Added IPv6Address fields for local and remote IP addresses
* (**quicksight**) This release adds support for the Cross Sheet Filter and Control features, and support for warnings in asset imports for any permitted errors encountered during execution
* (**rolesanywhere**) This release introduces the PutAttributeMapping and DeleteAttributeMapping APIs. IAM Roles Anywhere now provides the capability to define a set of mapping rules, allowing customers to specify which data is extracted from their X.509 end-entity certificates.
* (**sagemaker**) Removed deprecated enum values and updated API documentation.
* (**workspaces**) Adds new APIs for managing and sharing WorkSpaces BYOL configuration across accounts.

## [1.1.20] - 04/17/2024

### Features
* (**ec2**) Documentation updates for Elastic Compute Cloud (EC2).
* (**qbusiness**) This release adds support for IAM Identity Center (IDC) as the identity gateway for Q Business. It also allows users to provide an explicit intent for Q Business to identify how the Chat request should be handled.

## [1.1.19] - 04/16/2024

### Features
* (**bedrockagent**) For Create Agent API, the agentResourceRoleArn parameter is no longer required.
* (**emrserverless**) This release adds support for shuffle optimized disks that allow larger disk sizes and higher IOPS to efficiently run shuffle heavy workloads.
* (**entityresolution**) Cross Account Resource Support .
* (**iotwireless**) Add PublicGateways in the GetWirelessStatistics call response, indicating the LoRaWAN public network accessed by the device.
* (**lakeformation**) This release adds Lake Formation managed RAM support for the 4 APIs - "DescribeLakeFormationIdentityCenterConfiguration", "CreateLakeFormationIdentityCenterConfiguration", "DescribeLakeFormationIdentityCenterConfiguration", and "DeleteLakeFormationIdentityCenterConfiguration"
* (**m2**) Adding new ListBatchJobRestartPoints API and support for restart batch job.
* (**mediapackagev2**) Dash v2 is a MediaPackage V2 feature to support egressing on DASH manifest format.
* (**outposts**) This release adds new APIs to allow customers to configure their Outpost capacity at order-time.
* (**wellarchitected**) AWS Well-Architected now has a Connector for Jira to allow customers to efficiently track workload risks and improvement efforts and create closed-loop mechanisms.

## [1.1.18] - 04/12/2024

### Features
* (**cloudformation**) Adding support for the new parameter "IncludePropertyValues" in the CloudFormation DescribeChangeSet API. When this parameter is included, the DescribeChangeSet response will include more detailed information such as before and after values for the resource properties that will change.
* (**glue**) Modifying request for GetUnfilteredTableMetadata for view-related fields.
* (**healthlake**) Added new CREATE_FAILED status for data stores. Added new errorCause to DescribeFHIRDatastore API and ListFHIRDatastores API response for additional insights into data store creation and deletion workflows.
* (**kms**) This feature supports the ability to specify a custom rotation period for automatic key rotations, the ability to perform on-demand key rotations, and visibility into your key material rotations.
* (**mediatailor**) Added InsertionMode to PlaybackConfigurations. This setting controls whether players can use stitched or guided ad insertion. The default for players that do not specify an insertion mode is stitched.
* (**outposts**) This release adds EXPEDITORS as a valid shipment carrier.
* (**redshift**) Adds support for Amazon Redshift DescribeClusterSnapshots API to include Snapshot ARN response field.
* (**transfer**) This change releases support for importing self signed certificates to the Transfer Family for sending outbound file transfers over TLS/HTTPS.

### Documentation
* (**configservice**) Updates documentation for AWS Config
* (**iotfleethub**) Documentation updates for AWS IoT Fleet Hub to clarify that Fleet Hub supports organization instance of IAM Identity Center.
* (**neptunegraph**) Update to API documentation to resolve customer reported issues.

## [1.1.17] - 04/11/2024

### Features
* (**batch**) This release adds the task properties field to attempt details and the name field on EKS container detail.
* (**cloudfront**) CloudFront origin access control extends support to AWS Lambda function URLs and AWS Elemental MediaPackage v2 origins.
* (**cloudwatch**) This release adds support for Metric Characteristics for CloudWatch Anomaly Detection. Anomaly Detector now takes Metric Characteristics object with Periodic Spikes boolean field that tells Anomaly Detection that spikes that repeat at the same time every week are part of the expected pattern.
* (**iam**) For CreateOpenIDConnectProvider API, the ThumbprintList parameter is no longer required.
* (**medialive**) AWS Elemental MediaLive introduces workflow monitor, a new feature that enables the visualization and monitoring of your media workflows. Create signal maps of your existing workflows and monitor them by creating notification and monitoring template groups.
* (**omics**) This release adds support for retrieval of S3 direct access metadata on sequence stores and read sets, and adds support for SHA256up and SHA512up HealthOmics ETags.
* (**pipes**) LogConfiguration ARN validation fixes
* (**wafv2**) Adds an updated version of smoke tests, including smithy trait, for SDK testing.

### Documentation
* (**codebuild**) Support access tokens for Bitbucket sources
* (**rds**) Updates Amazon RDS documentation for Standard Edition 2 support in RDS Custom for Oracle.
* (**s3control**) Documentation updates for Amazon S3-control.

## [1.1.16] - 04/10/2024

### Features
* (**cleanrooms**) AWS Clean Rooms Differential Privacy is now fully available. Differential privacy protects against user-identification attempts.
* (**connect**) This release adds new Submit Auto Evaluation Action for Amazon Connect Rules.
* (**qconnect**) This release adds a new QiC public API updateSession and updates an existing QiC public API createSession
* (**rekognition**) Added support for ContentType to content moderation detections.
* (**supplychain**) This release includes API SendDataIntegrationEvent for AWS Supply Chain
* (**workspacesthinclient**) Adding tags field to SoftwareSet. Removing tags fields from Summary objects. Changing the list of exceptions in tagging APIs. Fixing an issue where the SDK returns empty tags in Get APIs.

### Documentation
* (**networkmonitor**) Examples were added to CloudWatch Network Monitor commands.

## [1.1.15] - 04/09/2024

### Features
* (**codebuild**) Add new webhook filter types for GitHub webhooks
* (**mediaconvert**) This release includes support for bringing your own fonts to use for burn-in or DVB-Sub captioning workflows.
* (**pinpoint**) The OrchestrationSendingRoleArn has been added to the email channel and is used to send emails from campaigns or journeys.
* (**rds**) This release adds support for specifying the CA certificate to use for the new db instance when restoring from db snapshot, restoring from s3, restoring to point in time, and creating a db instance read replica.

## [1.1.13] - 04/05/2024

### Features
* (**quicksight**) Adding IAMIdentityCenterInstanceArn parameter to CreateAccountSubscription
* (**resourcegroups**) Added a new QueryErrorCode RESOURCE_TYPE_NOT_SUPPORTED that is returned by the ListGroupResources operation if the group query contains unsupported resource types.
* (**verifiedpermissions**) Adding BatchIsAuthorizedWithToken API which supports multiple authorization requests against a PolicyStore given a bearer token.

## [1.1.12] - 04/04/2024

### Features
* (**b2bi**) Adding support for X12 5010 HIPAA EDI version and associated transaction sets.
* (**cleanrooms**) Feature: New schemaStatusDetails field to the existing Schema object that displays a status on Schema API responses to show whether a schema is queryable or not. New BatchGetSchemaAnalysisRule API to retrieve multiple schemaAnalysisRules using a single API call.
* (**ec2**) Amazon EC2 G6 instances powered by NVIDIA L4 Tensor Core GPUs can be used for a wide range of graphics-intensive and machine learning use cases. Gr6 instances also feature NVIDIA L4 GPUs and can be used for graphics workloads with higher memory requirements.
* (**emrcontainers**) This release adds support for integration with EKS AccessEntry APIs to enable automatic Cluster Access for EMR on EKS.
* (**ivs**) API update to include an SRT ingest endpoint and passphrase for all channels.
* (**verifiedpermissions**) Adds GroupConfiguration field to Identity Source API's

## [1.1.11] - 04/03/2024

### Features
* (**cleanroomsml**) The release includes a public SDK for AWS Clean Rooms ML APIs, making them globally available to developers worldwide.
* (**cloudformation**) This release would return a new field - PolicyAction in cloudformation's existed DescribeChangeSetResponse, showing actions we are going to apply on the physical resource (e.g., Delete, Retain) according to the user's template
* (**datazone**) This release supports the feature of dataQuality to enrich asset with dataQualityResult in Amazon DataZone.
* (**docdb**) This release adds Global Cluster Switchover capability which enables you to change your global cluster's primary AWS Region, the region that serves writes, while preserving the replication between all regions in the global cluster.
* (**groundstation**) This release adds visibilityStartTime and visibilityEndTime to DescribeContact and ListContacts responses.
* (**lambda**) Add Ruby 3.3 (ruby3.3) support to AWS Lambda
* (**medialive**) Cmaf Ingest outputs are now supported in Media Live
* (**medicalimaging**) SearchImageSets API now supports following enhancements - Additional support for searching on UpdatedAt and SeriesInstanceUID - Support for searching existing filters between dates/times - Support for sorting the search result by Ascending/Descending - Additional parameters returned in the response
* (**transfer**) Add ability to specify Security Policies for SFTP Connectors

## [1.1.10] - 04/02/2024

### Features
* (**glue**) Adding View related fields to responses of read-only Table APIs.
* (**rolesanywhere**) This release increases the limit on the roleArns request parameter for the *Profile APIs that support it. This parameter can now take up to 250 role ARNs.

### Documentation
* (**ecs**) Documentation only update for Amazon ECS.
* (**ivschat**) Doc-only update. Changed "Resources" to "Key Concepts" in docs and updated text.
* (**securityhub**) Documentation updates for AWS Security Hub

## [1.1.9] - 04/01/2024

### Features
* (**cloudwatch**) This release adds support for CloudWatch Anomaly Detection on cross-account metrics. SingleMetricAnomalyDetector and MetricDataQuery inputs to Anomaly Detection APIs now take an optional AccountId field.
* (**datazone**) This release supports the feature of AI recommendations for descriptions to enrich the business data catalog in Amazon DataZone.
* (**deadline**) AWS Deadline Cloud is a new fully managed service that helps customers set up, deploy, and scale rendering projects in minutes, so they can improve the efficiency of their rendering pipelines and take on more projects.
* (**lightsail**) This release adds support to upgrade the TLS version of the distribution.

### Documentation
* (**emr**) This release fixes a broken link in the documentation.

## [1.1.8] - 03/29/2024

### Features
* (**b2bi**) Supporting new EDI X12 transaction sets for X12 versions 4010, 4030, and 5010.
* (**codebuild**) Add new fleet status code for Reserved Capacity.
* (**codeconnections**) Duplicating the CodeStar Connections service into the new, rebranded AWS CodeConnections service.
* (**internetmonitor**) This release adds support to allow customers to track cross account monitors through ListMonitor, GetMonitor, ListHealthEvents, GetHealthEvent, StartQuery APIs.
* (**iotwireless**) Add support for retrieving key historical and live metrics for LoRaWAN devices and gateways
* (**marketplacecatalog**) This release enhances the ListEntities API to support ResaleAuthorizationId filter and sort for OfferEntity in the request and the addition of a ResaleAuthorizationId field in the response of OfferSummary.
* (**neptunegraph**) Add the new API Start-Import-Task for Amazon Neptune Analytics.
* (**sagemaker**) This release adds support for custom images for the CodeEditor App on SageMaker Studio

## [1.1.7] - 03/28/2024

### Features
* (**codecatalyst**) This release adds support for understanding pending changes to subscriptions by including two new response parameters for the GetSubscription API for Amazon CodeCatalyst.
* (**computeoptimizer**) This release enables AWS Compute Optimizer to analyze and generate recommendations with a new customization preference, Memory Utilization.
* (**ec2**) Amazon EC2 C7gd, M7gd and R7gd metal instances with up to 3.8 TB of local NVMe-based SSD block-level storage have up to 45% improved real-time NVMe storage performance than comparable Graviton2-based instances.
* (**eks**) Add multiple customer error code to handle customer caused failure when managing EKS node groups
* (**guardduty**) Add EC2 support for GuardDuty Runtime Monitoring auto management.
* (**neptunegraph**) Update ImportTaskCancelled waiter to evaluate task state correctly and minor documentation changes.
* (**oam**) This release adds support for sharing AWS::InternetMonitor::Monitor resources.
* (**quicksight**) Amazon QuickSight: Adds support for setting up VPC Endpoint restrictions for accessing QuickSight Website.

## [1.1.6] - 03/27/2024

### Features
* (**batch**) This feature allows AWS Batch to support configuration of imagePullSecrets and allowPrivilegeEscalation for jobs running on EKS
* (**bedrockagent**) This changes introduces metadata documents statistics and also updates the documentation for bedrock agent.
* (**bedrockagentruntime**) This release introduces filtering support on Retrieve and RetrieveAndGenerate APIs.
* (**elasticache**) Added minimum capacity to  Amazon ElastiCache Serverless. This feature allows customer to ensure minimum capacity even without current load

### Documentation
* (**secretsmanager**) Documentation updates for Secrets Manager

## [1.1.5] - 03/26/2024

### Features
* (**bedrockagentruntime**) This release adds support to customize prompts sent through the RetrieveAndGenerate API in Agents for Amazon Bedrock.
* (**costexplorer**) Adds support for backfill of cost allocation tags, with new StartCostAllocationTagBackfill and ListCostAllocationTagBackfillHistory API.
* (**ec2**) Documentation updates for Elastic Compute Cloud (EC2).
* (**finspace**) Add new operation delete-kx-cluster-node and add status parameter to list-kx-cluster-node operation.

### Documentation
* (**ecs**) This is a documentation update for Amazon ECS.

## [1.1.4] - 03/25/2024

### Features
* (**codebuild**) Supporting GitLab and GitLab Self Managed as source types in AWS CodeBuild.
* (**ec2**) Added support for ModifyInstanceMetadataDefaults and GetInstanceMetadataDefaults to set Instance Metadata Service account defaults
* (**emrcontainers**) This release increases the number of supported job template parameters from 20 to 100.
* (**globalaccelerator**) AWS Global Accelerator now supports cross-account sharing for bring your own IP addresses.
* (**medialive**) Exposing TileMedia H265 options
* (**sagemaker**) Introduced support for the following new instance types on SageMaker Studio for JupyterLab and CodeEditor applications: m6i, m6id, m7i, c6i, c6id, c7i, r6i, r6id, r7i, and p5

### Fixes
* Support client-configured `accountIdEndpointMode`

### Documentation
* (**ecs**) Documentation only update for Amazon ECS.

## [1.1.3] - 03/22/2024

### Features
* (**kendra**) Documentation update, March 2024. Corrects some docs for Amazon Kendra.
* (**pricing**) Add ResourceNotFoundException to ListPriceLists and GetPriceListFileUrl APIs
* (**rolesanywhere**) This release relaxes constraints on the durationSeconds request parameter for the *Profile APIs that support it. This parameter can now take on values that go up to 43200.
* (**securityhub**) Added new resource detail object to ASFF, including resource for LastKnownExploitAt

### Documentation
* (**firehose**) Updates Amazon Firehose documentation for message regarding Enforcing Tags IAM Policy.

## [1.1.2] - 03/21/2024

### Features
* (**codeartifact**) This release adds Package groups to CodeArtifact so you can more conveniently configure package origin controls for multiple packages.

## [1.1.1] - 03/20/2024

### Features
* (**accessanalyzer**) This release adds support for policy validation and external access findings for DynamoDB tables and streams. IAM Access Analyzer helps you author functional and secure resource-based policies and identify cross-account access. Updated service API, documentation, and paginators.
* (**connect**) This release updates the *InstanceStorageConfig APIs to support a new ResourceType: REAL_TIME_CONTACT_ANALYSIS_CHAT_SEGMENTS. Use this resource type to enable streaming for real-time analysis of chat contacts and to associate a Kinesis stream where real-time analysis chat segments will be published.
* (**dynamodb**) This release introduces 3 new APIs ('GetResourcePolicy', 'PutResourcePolicy' and 'DeleteResourcePolicy') and modifies the existing 'CreateTable' API for the resource-based policy support. It also modifies several APIs to accept a 'TableArn' for the 'TableName' parameter.
* (**managedblockchainquery**) AMB Query: update GetTransaction to include transactionId as input
* (**savingsplans**) Introducing the Savings Plans Return feature enabling customers to return their Savings Plans within 7 days of purchase.

### Documentation
* (**codebuild**) This release adds support for new webhook events (RELEASED and PRERELEASED) and filter types (TAG_NAME and RELEASE_NAME).

## [1.1.0] - 03/19/2024

### Features
* (**cloudwatchlogs**) Update LogSamples field in Anomaly model to be a list of LogEvent
* (**ec2**) This release adds the new DescribeMacHosts API operation for getting information about EC2 Mac Dedicated Hosts. Users can now see the latest macOS versions that their underlying Apple Mac can support without needing to be updated.
* (**finspace**) Adding new attributes readWrite and onDemand to dataview models for Database Maintenance operations.
* (**managedblockchainquery**) Introduces a new API for Amazon Managed Blockchain Query: ListFilteredTransactionEvents.

### Fixes
* [#1045](https://github.com/awslabs/smithy-kotlin/issues/1045) ⚠️ **IMPORTANT**: Fix codegen for map shapes which use string enums as map keys. See the [**Map key changes** breaking change announcement](https://github.com/awslabs/aws-sdk-kotlin/discussions/1258) for more details
* [#1041](https://github.com/awslabs/smithy-kotlin/issues/1041) ⚠️ **IMPORTANT**: Disable [OkHttp's transparent response decompression](https://square.github.io/okhttp/features/calls/#rewriting-requests) by manually specifying `Accept-Encoding: identity` in requests. See the [**Disabling automatic response decompression** breaking change announcement](https://github.com/awslabs/aws-sdk-kotlin/discussions/1259) for more details.

### Documentation
* (**cloudformation**) Documentation update, March 2024. Corrects some formatting.

## [1.0.80] - 03/18/2024

### Features
* (**cloudformation**) This release supports for a new API ListStackSetAutoDeploymentTargets, which provider auto-deployment configuration as a describable resource. Customers can now view the specific combinations of regions and OUs that are being auto-deployed.
* (**kms**) Adds the ability to use the default policy name by omitting the policyName parameter in calls to PutKeyPolicy and GetKeyPolicy
* (**mediatailor**) This release adds support to allow customers to show different content within a channel depending on metadata associated with the viewer.
* (**rds**) This release launches the ModifyIntegration API and support for data filtering for zero-ETL Integrations.
* (**s3**) Fix two issues with response root node names.

### Documentation
* (**timestreamquery**) Documentation updates, March 2024

## [1.0.79] - 03/15/2024

### Features
* (**backup**) This release introduces a boolean attribute ManagedByAWSBackupOnly as part of ListRecoveryPointsByResource api to filter the recovery points based on ownership. This attribute can be used to filter out the recovery points protected by AWSBackup.
* (**codebuild**) AWS CodeBuild now supports overflow behavior on Reserved Capacity.
* (**connect**) This release adds Hierarchy based Access Control fields to Security Profile public APIs and adds support for UserAttributeFilter to SearchUsers API.
* (**ec2**) Add media accelerator and neuron device information on the describe instance types API.
* (**kinesisanalyticsv2**) Support for Flink 1.18 in Managed Service for Apache Flink
* (**sagemaker**) Adds m6i, m6id, m7i, c6i, c6id, c7i, r6i r6id, r7i, p5 instance type support to Sagemaker Notebook Instances and miscellaneous wording fixes for previous Sagemaker documentation.
* (**workspacesthinclient**) Removed unused parameter kmsKeyArn from UpdateDeviceRequest

### Documentation
* (**s3**) Documentation updates for Amazon S3.

## [1.0.78] - 03/14/2024

### Features
* (**ec2instanceconnect**) This release includes a new exception type "SerialConsoleSessionUnsupportedException" for SendSerialConsoleSSHPublicKey API.
* (**fis**) This release adds support for previewing target resources before running a FIS experiment. It also adds resource ARNs for actions, experiments, and experiment templates to API responses.
* (**timestreaminfluxdb**) This is the initial SDK release for Amazon Timestream for InfluxDB. Amazon Timestream for InfluxDB is a new time-series database engine that makes it easy for application developers and DevOps teams to run InfluxDB databases on AWS for near real-time time-series applications using open source APIs.

### Documentation
* (**amplify**) Documentation updates for Amplify. Identifies the APIs available only to apps created using Amplify Gen 1.
* (**elasticloadbalancingv2**) This release allows you to configure HTTP client keep-alive duration for communication between clients and Application Load Balancers.
* (**rds**) Updates Amazon RDS documentation for EBCDIC collation for RDS for Db2.
* (**secretsmanager**) Doc only update for Secrets Manager

### Miscellaneous
* Remove IoT RoboRunner service

## [1.0.77] - 03/13/2024

### Features
* (**ivsrealtime**) adds support for multiple new composition layout configuration options (grid, pip)
* (**kinesisanalyticsv2**) Support new RuntimeEnvironmentUpdate parameter within UpdateApplication API allowing callers to change the Flink version upon which their application runs.
* (**s3**) This release makes the default option for S3 on Outposts request signing to use the SigV4A algorithm when using AWS Common Runtime (CRT).

## [1.0.76] - 03/12/2024

### Features
* (**connect**) This release increases MaxResults limit to 500 in request for SearchUsers, SearchQueues and SearchRoutingProfiles APIs of Amazon Connect.
* (**kafka**) Added support for specifying the starting position of topic replication in MSK-Replicator.

### Documentation
* (**cloudformation**) CloudFormation documentation update for March, 2024
* (**ec2**) Documentation updates for Amazon EC2.
* (**ssm**) March 2024 doc-only updates for Systems Manager.

## [1.0.75] - 03/11/2024

### Features
* (**codestarconnections**) Added a sync configuration enum to disable publishing of deployment status to source providers (PublishDeploymentStatus). Added a sync configuration enum (TriggerStackUpdateOn) to only trigger changes.
* (**mediapackagev2**) This release enables customers to safely update their MediaPackage v2 channel groups, channels and origin endpoints using entity tags.
* Added the `sigV4aSigningRegionSet` configuration option

### Documentation
* (**elasticache**) Revisions to API text that are now to be carried over to SDK text, changing usages of "SFO" in code examples to "us-west-1", and some other typos.

### Miscellaneous
* Bump smithy version to 1.45.0

## [1.0.74] - 03/08/2024

### Features
* (**batch**) This release adds JobStateTimeLimitActions setting to the Job Queue API. It allows you to configure an action Batch can take for a blocking job in front of the queue after the defined period of time. The new parameter applies for ECS, EKS, and FARGATE Job Queues.
* (**cloudtrail**) Added exceptions to CreateTrail, DescribeTrails, and ListImportFailures APIs.
* (**cognitoidentityprovider**) Add ConcurrentModificationException to SetUserPoolMfaConfig
* (**guardduty**) Add RDS Provisioned and Serverless Usage types
* (**transfer**) Added DES_EDE3_CBC to the list of supported encryption algorithms for messages sent with an AS2 connector.
* [#1212](https://github.com/awslabs/aws-sdk-kotlin/issues/1212) Add request IDs to exception messages where available
* [#1212](https://github.com/awslabs/aws-sdk-kotlin/issues/1212) Add error metadata to ServiceException messages when a service-provided message isn't available

### Documentation
* (**bedrockagentruntime**) Documentation update for Bedrock Runtime Agent
* (**codebuild**) This release adds support for a new webhook event: PULL_REQUEST_CLOSED.

## [1.0.73] - 03/07/2024

### Features
* (**appconfig**) AWS AppConfig now supports dynamic parameters, which enhance the functionality of AppConfig Extensions by allowing you to provide parameter values to your Extensions at the time you deploy your configuration.
* (**ec2**) This release adds an optional parameter to RegisterImage and CopyImage APIs to support tagging AMIs at the time of creation.
* (**grafana**) Adds support for the new GrafanaToken as part of the Amazon Managed Grafana Enterprise plugins upgrade to associate your AWS account with a Grafana Labs account.
* (**paymentcryptographydata**) AWS Payment Cryptography EMV Decrypt Feature  Release
* (**wafv2**) You can increase the max request body inspection size for some regional resources. The size setting is in the web ACL association config. Also, the AWSManagedRulesBotControlRuleSet EnableMachineLearning setting now takes a Boolean instead of a primitive boolean type, for languages like Java.

### Documentation
* (**lambda**) Documentation updates for AWS Lambda
* (**rds**) Updates Amazon RDS documentation for io2 storage for Multi-AZ DB clusters
* (**snowball**) Doc-only update for change to EKS-Anywhere ordering.
* (**workspaces**) Added note for user decoupling

## [1.0.72] - 03/06/2024

### Features
* (**imagebuilder**) Add PENDING status to Lifecycle Execution resource status. Add StartTime and EndTime to ListLifecycleExecutionResource API response.
* (**rds**) Updated the input of CreateDBCluster and ModifyDBCluster to support setting CA certificates. Updated the output of DescribeDBCluster to show current CA certificate setting value.
* (**verifiedpermissions**) Deprecating details in favor of configuration for GetIdentitySource and ListIdentitySources APIs.

### Documentation
* (**dynamodb**) Doc only updates for DynamoDB documentation
* (**mwaa**) Amazon MWAA adds support for Apache Airflow v2.8.1.
* (**redshift**) Update for documentation only. Covers port ranges, definition updates for data sharing, and definition updates to cluster-snapshot documentation.

## [1.0.71] - 03/05/2024

### Features
* (**organizations**) This release contains an endpoint addition
* (**sesv2**) Adds support for providing custom headers within SendEmail and SendBulkEmail for SESv2.

### Documentation
* (**apigateway**) Documentation updates for Amazon API Gateway
* (**chatbot**) Minor update to documentation.

## [1.0.70] - 03/04/2024

### Features
* (**cloudformation**) Add DetailedStatus field to DescribeStackEvents and DescribeStacks APIs
* (**fsx**) Added support for creating FSx for NetApp ONTAP file systems with up to 12 HA pairs, delivering up to 72 GB/s of read throughput and 12 GB/s of write throughput.
* (**organizations**) Documentation update for AWS Organizations

## [1.0.69] - 03/01/2024

### Documentation
* (**accessanalyzer**) Fixed a typo in description field.
* (**autoscaling**) With this release, Amazon EC2 Auto Scaling groups, EC2 Fleet, and Spot Fleet improve the default price protection behavior of attribute-based instance type selection of Spot Instances, to consistently select from a wide range of instance types.
* (**ec2**) With this release, Amazon EC2 Auto Scaling groups, EC2 Fleet, and Spot Fleet improve the default price protection behavior of attribute-based instance type selection of Spot Instances, to consistently select from a wide range of instance types.

## [1.0.68] - 02/29/2024

### Features
* (**docdbelastic**) Launched Elastic Clusters Readable Secondaries, Start/Stop, Configurable Shard Instance count, Automatic Backups and Snapshot Copying
* (**eks**) Added support for new AL2023 AMIs to the supported AMITypes.
* (**lexmodelsv2**) This release makes AMAZON.QnAIntent generally available in Amazon Lex. This generative AI feature leverages large language models available through Amazon Bedrock to automate frequently asked questions (FAQ) experience for end-users.
* (**migrationhuborchestrator**) Adds new CreateTemplate, UpdateTemplate and DeleteTemplate APIs.
* (**quicksight**) TooltipTarget for Combo chart visuals; ColumnConfiguration limit increase to 2000; Documentation Update
* (**sagemaker**) Adds support for ModelDataSource in Model Packages to support unzipped models. Adds support to specify SourceUri for models which allows registration of models without mandating a container for hosting. Using SourceUri, customers can decouple the model from hosting information during registration.
* (**securitylake**) Add capability to update the Data Lake's MetaStoreManager Role in order to perform required data lake updates to use Iceberg table format in their data lake or update the role for any other reason.
* Add support for S3 Express One Zone

### Fixes
* Fix an issue where sections were not properly divided when parsing the config file
* [#1220](https://github.com/awslabs/aws-sdk-kotlin/issues/1220) Refactor XML deserialization to handle flat collections

## [1.0.67] - 02/28/2024

### Features
* (**batch**) This release adds Batch support for configuration of multicontainer jobs in ECS, Fargate, and EKS. This support is available for all types of jobs, including both array jobs and multi-node parallel jobs.
* (**bedrockagentruntime**) This release adds support to override search strategy performed by the Retrieve and RetrieveAndGenerate APIs for Amazon Bedrock Agents
* (**costexplorer**) This release introduces the new API 'GetApproximateUsageRecords', which retrieves estimated usage records for hourly granularity or resource-level data at daily granularity.
* (**ec2**) This release increases the range of MaxResults for GetNetworkInsightsAccessScopeAnalysisFindings to 1,000.
* (**iot**) This release reduces the maximum results returned per query invocation from 500 to 100 for the SearchIndex API. This change has no implications as long as the API is invoked until the nextToken is NULL.
* (**wafv2**) AWS WAF now supports configurable time windows for request aggregation with rate-based rules. Customers can now select time windows of 1 minute, 2 minutes or 10 minutes, in addition to the previously supported 5 minutes.

## [1.0.66] - 02/27/2024

### Features
* (**amplifyuibuilder**) We have added the ability to tag resources after they are created

## [1.0.65] - 02/26/2024

### Features
* (**drs**) Added volume status to DescribeSourceServer replicated volumes.
* (**kafkaconnect**) Adds support for tagging, with new TagResource, UntagResource and ListTagsForResource APIs to manage tags and updates to existing APIs to allow tag on create. This release also adds support for the new DeleteWorkerConfiguration API.
* (**rds**) This release adds support for gp3 data volumes for Multi-AZ DB Clusters.

### Documentation
* (**apigateway**) Documentation updates for Amazon API Gateway.

## [1.0.64] - 02/23/2024

### Features
* (**rds**) Add pattern and length based validations for DBShardGroupIdentifier

### Documentation
* (**appsync**) Documentation only updates for AppSync
* (**qldb**) Clarify possible values for KmsKeyArn and EncryptionDescription.
* (**rum**) Doc-only update for new RUM metrics that were added

## [1.0.63] - 02/22/2024

### Features
* (**internetmonitor**) This release adds IPv4 prefixes to health events
* (**kinesisvideo**) Increasing NextToken parameter length restriction for List APIs from 512 to 1024.

## [1.0.62] - 02/21/2024

### Features
* (**iotevents**) Increase the maximum length of descriptions for Inputs, Detector Models, and Alarm Models
* (**lookoutequipment**) This release adds a field exposing model quality to read APIs for models. It also adds a model quality field to the API response when creating an inference scheduler.
* (**medialive**) MediaLive now supports the ability to restart pipelines in a running channel.
* (**ssm**) This release adds support for sharing Systems Manager parameters with other AWS accounts.

### Fixes
* [#1208](https://github.com/awslabs/aws-sdk-kotlin/issues/1208) Profile credentials provider is no longer marked as internal API
* [#1217](https://github.com/awslabs/aws-sdk-kotlin/issues/1217) Only enable aws-chunked content encoding for S3

## [1.0.61] - 02/20/2024

### Features
* (**firehose**) This release updates a few Firehose related APIs.
* (**lambda**) Add .NET 8 (dotnet8) Runtime support to AWS Lambda.

### Documentation
* (**dynamodb**) Publishing quick fix for doc only update.

## [1.0.60] - 02/19/2024

### Features
* (**amplify**) This release contains API changes that enable users to configure their Amplify domains with their own custom SSL/TLS certificate.
* (**chatbot**) This release adds support for AWS Chatbot. You can now monitor, operate, and troubleshoot your AWS resources with interactive ChatOps using the AWS SDK.
* (**mediatailor**) MediaTailor: marking #AdBreak.OffsetMillis as required.

### Documentation
* (**configservice**) Documentation updates for the AWS Config CLI
* (**ivs**) Changed description for latencyMode in Create/UpdateChannel and Channel/ChannelSummary.
* (**keyspaces**) Documentation updates for Amazon Keyspaces

## [1.0.59] - 02/16/2024

### Features
* (**emr**) adds fine grained control over Unhealthy Node Replacement to Amazon ElasticMapReduce
* (**firehose**) This release adds support for Data Message Extraction for decompressed CloudWatch logs, and to use a custom file extension or time zone for S3 destinations.
* (**sns**) This release marks phone numbers as sensitive inputs.

### Documentation
* (**connectparticipant**) Doc only update to GetTranscript API reference guide to inform users about presence of events in the chat transcript.
* (**lambda**) Documentation-only updates for Lambda to clarify a number of existing actions and properties.
* (**rds**) Doc only update for a valid option in DB parameter group

## [1.0.58] - 02/15/2024

### Features
* (**artifact**) This is the initial SDK release for AWS Artifact. AWS Artifact provides on-demand access to compliance and third-party compliance reports. This release includes access to List and Get reports, along with their metadata. This release also includes access to AWS Artifact notifications settings.
* (**codepipeline**) Add ability to override timeout on action level.
* (**guardduty**) Marked fields IpAddressV4, PrivateIpAddress, Email as Sensitive.
* (**healthlake**) This release adds a new response parameter, JobProgressReport, to the DescribeFHIRImportJob and ListFHIRImportJobs API operation. JobProgressReport provides details on the progress of the import job on the server.
* (**opensearch**) Adds additional supported instance types.
* (**polly**) Amazon Polly adds 1 new voice - Burcu (tr-TR)
* (**sagemaker**) This release adds a new API UpdateClusterSoftware for SageMaker HyperPod. This API allows users to patch HyperPod clusters with latest platform softwares.

### Documentation
* (**detective**) Doc only updates for content enhancement
* (**secretsmanager**) Doc only update for Secrets Manager

## [1.0.57] - 02/14/2024

### Features
* (**controltower**) Adds support for new Baseline and EnabledBaseline APIs for automating multi-account governance.
* (**lookoutequipment**) This feature allows customers to see pointwise model diagnostics results for their models.
* (**qbusiness**) This release adds the metadata-boosting feature, which allows customers to easily fine-tune the underlying ranking of retrieved RAG passages in order to optimize Q&A answer relevance. It also adds new feedback reasons for the PutFeedback API.

## [1.0.56] - 02/13/2024

### Features
* (**lightsail**) This release adds support to upgrade the major version of a database.
* (**marketplacecatalog**) AWS Marketplace Catalog API now supports setting intent on requests
* (**resourceexplorer2**) Resource Explorer now uses newly supported IPv4 'amazonaws.com' endpoints by default.

### Documentation
* (**securitylake**) Documentation updates for Security Lake

## [1.0.55] - 02/12/2024

### Features
* (**appsync**) Adds support for new options on GraphqlAPIs, Resolvers and  Data Sources for emitting Amazon CloudWatch metrics for enhanced monitoring of AppSync APIs.
* (**cloudwatch**) This release enables PutMetricData API request payload compression by default.
* (**neptunegraph**) Adding a new option "parameters" for data plane api ExecuteQuery to support running parameterized query via SDK.
* (**route53domains**) This release adds bill contact support for RegisterDomain, TransferDomain, UpdateDomainContact and GetDomainDetail API.

## [1.0.54] - 02/09/2024

### Features
* (**batch**) This feature allows Batch to support configuration of repository credentials for jobs running on ECS
* (**braket**) Creating a job will result in DeviceOfflineException when using an offline device, and DeviceRetiredException when using a retired device.
* (**costoptimizationhub**) Adding includeMemberAccounts field to the response of ListEnrollmentStatuses API.
* (**iot**) This release allows AWS IoT Core users to enable Online Certificate Status Protocol (OCSP) Stapling for TLS X.509 Server Certificates when creating and updating AWS IoT Domain Configurations with Custom Domain.
* (**pricing**) Add Throttling Exception to all APIs.

### Documentation
* (**amp**) Overall documentation updates.
* (**ecs**) Documentation only update for Amazon ECS.

## [1.0.53] - 02/08/2024

### Features
* (**codepipeline**) Add ability to execute pipelines with new parallel & queued execution modes and add support for triggers with filtering on branches and file paths.
* (**quicksight**) General Interactions for Visuals; Waterfall Chart Color Configuration; Documentation Update
* (**workspaces**) This release introduces User-Decoupling feature. This feature allows Workspaces Core customers to provision workspaces without providing users. CreateWorkspaces and DescribeWorkspaces APIs will now take a new optional parameter "WorkspaceName".

### Fixes
* [#1031](https://github.com/awslabs/smithy-kotlin/issues/1031) Bump **smithy-kotlin** version to consume upstream fixes for URL parsing

## [1.0.52] - 02/07/2024

### Features
* (**datasync**) AWS DataSync now supports manifests for specifying files or objects to transfer.
* (**lexmodelsv2**) This release introduces a new bot replication feature as part of Lex Global Resiliency offering. This feature leverages a new set of APIs that allow customers to create bot replicas and replicate changes to bots across regions.
* (**redshift**) LisRecommendations API to fetch Amazon Redshift Advisor recommendations.

## [1.0.51] - 02/06/2024

### Features
* (**appsync**) Support for environment variables in AppSync GraphQL APIs
* (**cloudwatchlogs**) This release adds a new field, logGroupArn, to the response of the logs:DescribeLogGroups action.
* (**elasticsearchservice**) This release adds clear visibility to the customers on the changes that they make on the domain.
* (**opensearch**) This release adds clear visibility to the customers on the changes that they make on the domain.
* (**wafv2**) You can now delete an API key that you've created for use with your CAPTCHA JavaScript integration API.

### Documentation
* (**ecs**) This release is a documentation only update to address customer issues.

## [1.0.50] - 02/05/2024

### Features
* (**glue**) Introduce Catalog Encryption Role within Glue Data Catalog Settings. Introduce SASL/PLAIN as an authentication method for Glue Kafka connections

### Documentation
* (**workspaces**) Added definitions of various WorkSpace states

## [1.0.49] - 02/02/2024

### Features
* (**sagemaker**) Amazon SageMaker Canvas adds GenerativeAiSettings support for CanvasAppSettings.

### Documentation
* (**dynamodb**) Any number of users can execute up to 50 concurrent restores (any type of restore) in a given account.

## [1.0.48] - 02/01/2024

### Features
* (**cognitoidentityprovider**) Added CreateIdentityProvider and UpdateIdentityProvider details for new SAML IdP features
* (**ivs**) This release introduces a new resource Playback Restriction Policy which can be used to geo-restrict or domain-restrict channel stream playback when associated with a channel.  New APIs to support this resource were introduced in the form of Create/Delete/Get/Update/List.
* (**managedblockchainquery**) This release adds support for transactions that have not reached finality. It also removes support for the status property from the response of the GetTransaction operation. You can use the confirmationStatus and executionStatus properties to determine the status of the transaction.
* (**mediaconvert**) This release includes support for broadcast-mixed audio description tracks.
* (**neptunegraph**) Adding new APIs in SDK for Amazon Neptune Analytics. These APIs include operations to execute, cancel, list queries and get the graph summary.
* [#476](https://github.com/awslabs/aws-sdk-kotlin/issues/476) Allow full URI path to a localhost metadata service (AwsContainerCredentialsFullUri) to be a host name

### Fixes
* Bump **smithy-kotlin** version to fix an error with serializing maps which use the `Document` type as a value

## [1.0.47] - 01/31/2024

### Features
* (**cloudformation**) CloudFormation IaC generator allows you to scan existing resources in your account and select resources to generate a template for a new or existing CloudFormation stack.
* (**elasticloadbalancingv2**) This release enables unhealthy target draining intervals for Network Load Balancers.
* (**glue**) Update page size limits for GetJobRuns and GetTriggers APIs.
* (**ssm**) This release adds an optional Duration parameter to StateManager Associations. This allows customers to specify how long an apply-only-on-cron association execution should run. Once the specified Duration is out all the ongoing cancellable commands or automations are cancelled.

## [1.0.46] - 01/30/2024

### Features
* (**datazone**) Add new skipDeletionCheck to DeleteDomain. Add new skipDeletionCheck to DeleteProject which also automatically deletes dependent objects

### Documentation
* (**route53**) Update the SDKs for text changes in the APIs.

## [1.0.45] - 01/29/2024

### Features
* (**autoscaling**) EC2 Auto Scaling customers who use attribute based instance-type selection can now intuitively define their Spot instances price protection limit as a percentage of the lowest priced On-Demand instance type.
* (**ec2**) EC2 Fleet customers who use attribute based instance-type selection can now intuitively define their Spot instances price protection limit as a percentage of the lowest priced On-Demand instance type.
* (**mwaa**) This release adds MAINTENANCE environment status for Amazon MWAA environments.
* (**rds**) Introduced support for the InsufficientDBInstanceCapacityFault error in the RDS RestoreDBClusterFromSnapshot and RestoreDBClusterToPointInTime API methods. This provides enhanced error handling, ensuring a more robust experience.

### Documentation
* (**comprehend**) Comprehend PII analysis now supports Spanish input documents.
* (**snowball**) Modified description of createaddress to include direction to add path when providing a JSON file.

## [1.0.44] - 01/26/2024

### Features
* (**connect**) Update list and string length limits for predefined attributes.
* (**inspector2**) This release adds ECR container image scanning based on their lastRecordedPullTime.
* (**sagemaker**) Amazon SageMaker Automatic Model Tuning now provides an API to programmatically delete tuning jobs.

## [1.0.43] - 01/25/2024

### Features
* (**acmpca**) AWS Private CA now supports an option to omit the CDP extension from issued certificates, when CRL revocation is enabled.
* (**lightsail**) This release adds support for IPv6-only instance plans.

## [1.0.42] - 01/24/2024

### Features
* (**ec2**) Introduced a new clientToken request parameter on CreateNetworkAcl and CreateRouteTable APIs. The clientToken parameter allows idempotent operations on the APIs.
* (**outposts**) DeviceSerialNumber parameter is now optional in StartConnection API
* (**rds**) This release adds support for Aurora Limitless Database.
* (**storagegateway**) Add DeprecationDate and SoftwareVersion to response of ListGateways.

### Fixes
* Fix application of sigv4a authentication scheme for S3, Eventbridge, and CloudFront KeyValueStore

### Documentation
* (**ecs**) Documentation updates for Amazon ECS.

### Miscellaneous
* Bump smithy-kotlin version to 1.0.11

## [1.0.41] - 01/23/2024

### Features
* (**inspector2**) This release adds support for CIS scans on EC2 instances.

### Fixes
* [#1187](https://github.com/awslabs/aws-sdk-kotlin/issues/1187) (**s3control**) Add missing `x-amz-content-sha256` header for SigV4 requests.

## [1.0.40] - 01/22/2024

### Features
* (**appconfigdata**) Fix FIPS Endpoints in aws-us-gov.
* (**cloudfrontkeyvaluestore**) This release improves upon the DescribeKeyValueStore API by returning two additional fields, Status of the KeyValueStore and the FailureReason in case of failures during creation of KeyValueStore.
* (**connectcases**) This release adds the ability to view audit history on a case and introduces a new parameter, performedBy, for CreateCase and UpdateCase API's.
* (**ecs**) This release adds support for Transport Layer Security (TLS) and Configurable Timeout to ECS Service Connect. TLS facilitates privacy and data security for inter-service communications, while Configurable Timeout allows customized per-request timeout and idle timeout for Service Connect services.
* (**finspace**) Allow customer to set zip default through command line arguments.
* (**rds**) Introduced support for the InsufficientDBInstanceCapacityFault error in the RDS CreateDBCluster API method. This provides enhanced error handling, ensuring a more robust experience when creating database clusters with insufficient instance capacity.

### Fixes
* Pass client-configured region to StsWebIdentityCredentialsProvider

### Documentation
* (**cloud9**) Doc-only update around removing AL1 from list of available AMIs for Cloud9
* (**ec2**) Documentation updates for Amazon EC2.
* (**organizations**) Doc only update for quota increase change

## [1.0.39] - 01/19/2024

### Features
* (**athena**) Introducing new NotebookS3LocationUri parameter to Athena ImportNotebook API. Payload is no longer required and either Payload or NotebookS3LocationUri needs to be provided (not both) for a successful ImportNotebook API call. If both are provided, an InvalidRequestException will be thrown.
* (**codebuild**) Release CodeBuild Reserved Capacity feature
* (**dynamodb**) This release adds support for including ApproximateCreationDateTimePrecision configurations in EnableKinesisStreamingDestination API, adds the same as an optional field in the response of DescribeKinesisStreamingDestination, and adds support for a new UpdateKinesisStreamingDestination API.
* (**qconnect**) Increased Quick Response name max length to 100

## [1.0.38] - 01/18/2024

### Features
* (**b2bi**) Increasing TestMapping inputFileContent file size limit to 5MB and adding file size limit 250KB for TestParsing input file. This release also includes exposing InternalServerException for Tag APIs.
* (**cloudtrail**) This release adds a new API ListInsightsMetricData to retrieve metric data from CloudTrail Insights.
* (**connect**) GetMetricDataV2 now supports 3 groupings
* (**drs**) Removed invalid and unnecessary default values.
* (**firehose**) Allow support for Snowflake as a Kinesis Data Firehose delivery destination.
* (**sagemakerfeaturestoreruntime**) Increase BatchGetRecord limits from 10 items to 100 items

## [1.0.37] - 01/17/2024

### Features
* (**keyspaces**) This release adds support for Multi-Region Replication with provisioned tables, and Keyspaces auto scaling APIs

### Documentation
* (**dynamodb**) Updating note for enabling streams for UpdateTable.

## [1.0.36] - 01/16/2024

### Features
* (**iot**) Revert release of LogTargetTypes
* (**iotfleetwise**) Updated APIs: SignalNodeType query parameter has been added to ListSignalCatalogNodesRequest and ListVehiclesResponse has been extended with attributes field.
* (**macie2**) This release adds support for analyzing Amazon S3 objects that are encrypted using dual-layer server-side encryption with AWS KMS keys (DSSE-KMS). It also adds support for reporting DSSE-KMS details in statistics and metadata about encryption settings for S3 buckets and objects.
* (**paymentcryptography**) Provide an additional option for key exchange using RSA wrap/unwrap in addition to tr-34/tr-31 in ImportKey and ExportKey operations. Added new key usage (type) TR31_M1_ISO_9797_1_MAC_KEY, for use with Generate/VerifyMac dataplane operations  with ISO9797 Algorithm 1 MAC calculations.
* (**rekognition**) This release adds ContentType and TaxonomyLevel attributes to DetectModerationLabels and GetMediaAnalysisJob API responses.

### Fixes
* [#1177](https://github.com/awslabs/aws-sdk-kotlin/issues/1177) Bump **smithy-kotlin** version to correct S3 presigning issues with custom endpoint ports

### Documentation
* (**personalize**) Documentation updates for Amazon Personalize.
* (**personalizeruntime**) Documentation updates for Amazon Personalize
* (**securityhub**) Documentation updates for AWS Security Hub

## [1.0.35] - 01/14/2024

### Features
* (**sagemaker**) This release will have ValidationException thrown if certain invalid app types are provided. The release will also throw ValidationException if more than 10 account ids are provided in VpcOnlyTrustedAccounts.

## [1.0.34] - 01/12/2024

### Features
* (**connect**) Supervisor Barge for Chat is now supported through the MonitorContact API.
* (**connectparticipant**) Introduce new Supervisor participant role
* (**mwaa**) This Amazon MWAA feature release includes new fields in CreateWebLoginToken response model. The new fields IamIdentity and AirflowIdentity will let you match identifications, as the Airflow identity length is currently hashed to 64 characters.
* (**s3control**) S3 On Outposts team adds dualstack endpoints support for S3Control and S3Outposts API calls.
* (**supplychain**) This release includes APIs CreateBillOfMaterialsImportJob and GetBillOfMaterialsImportJob.
* (**transfer**) AWS Transfer Family now supports static IP addresses for SFTP & AS2 connectors and for async MDNs on AS2 servers.

### Documentation
* (**location**) Location SDK documentation update. Added missing fonts to the MapConfiguration data type. Updated note for the SubMunicipality property in the place data type.

## [1.0.33] - 01/11/2024

### Features
* (**ec2**) This release adds support for adding an ElasticBlockStorage volume configurations in ECS RunTask/StartTask/CreateService/UpdateService APIs. The configuration allows for attaching EBS volumes to ECS Tasks.
* (**ecs**) This release adds support for adding an ElasticBlockStorage volume configurations in ECS RunTask/StartTask/CreateService/UpdateService APIs. The configuration allows for attaching EBS volumes to ECS Tasks.
* (**eventbridge**) Adding AppSync as an EventBridge Target
* (**iot**) Add ConflictException to Update APIs of AWS IoT Software Package Catalog
* (**iotfleetwise**) The following dataTypes have been removed: CUSTOMER_DECODED_INTERFACE in NetworkInterfaceType; CUSTOMER_DECODED_SIGNAL_INFO_IS_NULL in SignalDecoderFailureReason; CUSTOMER_DECODED_SIGNAL_NETWORK_INTERFACE_INFO_IS_NULL in NetworkInterfaceFailureReason; CUSTOMER_DECODED_SIGNAL in SignalDecoderType

### Documentation
* (**secretsmanager**) Doc only update for Secrets Manager
* (**workspaces**) Added AWS Workspaces RebootWorkspaces API - Extended Reboot documentation update

## [1.0.32] - 01/10/2024

### Features
* (**cloudwatchlogs**) Add support for account level subscription filter policies to PutAccountPolicy, DescribeAccountPolicies, and DeleteAccountPolicy APIs. Additionally, PutAccountPolicy has been modified with new optional "selectionCriteria" parameter for resource selection.
* (**connectcampaigns**) Minor pattern updates for Campaign and Dial Request API fields.
* (**location**) This release adds API support for custom layers for the maps service APIs: CreateMap, UpdateMap, DescribeMap.
* (**qconnect**) QueryAssistant and GetRecommendations will be discontinued starting June 1, 2024. To receive generative responses after March 1, 2024 you will need to create a new Assistant in the Connect console and integrate the Amazon Q in Connect JavaScript library (amazon-q-connectjs) into your applications.
* (**route53**) Route53 now supports geoproximity routing in AWS regions
* (**wisdom**) QueryAssistant and GetRecommendations will be discontinued starting June 1, 2024. To receive generative responses after March 1, 2024 you will need to create a new Assistant in the Connect console and integrate the Amazon Q in Connect JavaScript library (amazon-q-connectjs) into your applications.

### Fixes
* [#1173](https://github.com/awslabs/aws-sdk-kotlin/issues/1173) Correctly presign S3 requests when `forcePathStyle = true`

### Documentation
* (**redshiftserverless**) Updates to ConfigParameter for RSS workgroup, removal of use_fips_ssl

## [1.0.31] - 01/08/2024

### Features
* (**codebuild**) Aws CodeBuild now supports new compute type BUILD_GENERAL1_XLARGE
* (**ec2**) Amazon EC2 R7iz bare metal instances are powered by custom 4th generation Intel Xeon Scalable processors.
* (**route53resolver**) This release adds support for query type configuration on firewall rules that enables customers for granular action (ALLOW, ALERT, BLOCK) by DNS query type.

## [1.0.30] - 01/05/2024

### Features
* (**connect**) Minor trait updates for User APIs
* (**qconnect**) Marked SearchQuickResponses API as readonly.

### Documentation
* (**kms**) Documentation updates for AWS Key Management Service (KMS).
* (**redshiftserverless**) use_fips_ssl and require_ssl parameter support for Workgroup, UpdateWorkgroup, and CreateWorkgroup

## [1.0.29] - 01/04/2024

### Features
* (**configservice**) Updated ResourceType enum with new resource types onboarded by AWS Config in November and December 2023.
* (**docdb**) Adding PerformanceInsightsEnabled and PerformanceInsightsKMSKeyId fields to DescribeDBInstances Response.
* (**ecs**) This release adds support for managed instance draining which facilitates graceful termination of Amazon ECS instances.
* (**elasticsearchservice**) This release adds support for new or existing Amazon OpenSearch domains to enable TLS 1.3 or TLS 1.2 with perfect forward secrecy cipher suites for domain endpoints.
* (**lightsail**) This release adds support to set up an HTTPS endpoint on an instance.
* (**opensearch**) This release adds support for new or existing Amazon OpenSearch domains to enable TLS 1.3 or TLS 1.2 with perfect forward secrecy cipher suites for domain endpoints.
* (**sagemaker**) Adding support for provisioned throughput mode for SageMaker Feature Groups
* (**servicecatalog**) Added Idempotency token support to Service Catalog  AssociateServiceActionWithProvisioningArtifact, DisassociateServiceActionFromProvisioningArtifact, DeleteServiceAction API

## [1.0.28] - 01/03/2024

### Features
* (**connect**) Amazon Connect, Contact Lens Evaluation API increase evaluation notes max length to 3072.
* (**mediaconvert**) This release includes video engine updates including HEVC improvements, support for ingesting VP9 encoded video in MP4 containers, and support for user-specified 3D LUTs.

## [1.0.27] - 12/29/2023

### Features
* (**apprunner**) AWS App Runner adds Python 3.11 and Node.js 18 runtimes.
* (**location**) This release introduces a new parameter to bypasses an API key's expiry conditions and delete the key.
* (**quicksight**) Add LinkEntityArn support for different partitions; Add UnsupportedUserEditionException in UpdateDashboardLinks API; Add support for New Reader Experience Topics

### Fixes
* [#1165](https://github.com/awslabs/aws-sdk-kotlin/issues/1165) (**s3**) Fix default execution context attributes

### Miscellaneous
* Bump smithy version to 1.42.0

## [1.0.26] - 12/28/2023

### Features
* (**codestarconnections**) New integration with the GitLab self-managed provider type.
* (**kinesisvideoarchivedmedia**) NoDataRetentionException thrown when GetImages requested for a Stream that does not retain data (that is, has a DataRetentionInHours of 0).
* (**sagemaker**) Amazon SageMaker Studio now supports Docker access from within app container

## [1.0.25] - 12/27/2023

### Features
* (**emr**) Add support for customers to modify cluster attribute auto-terminate post cluster launch

## [1.0.24] - 12/26/2023

### Documentation
* (**iam**) Documentation updates for AWS Identity and Access Management (IAM).

## [1.0.23] - 12/22/2023

### Features
* (**bedrockagent**) Adding Claude 2.1 support to Bedrock Agents
* (**glue**) This release adds additional configurations for Query Session Context on the following APIs: GetUnfilteredTableMetadata, GetUnfilteredPartitionMetadata, GetUnfilteredPartitionsMetadata.
* (**lakeformation**) This release adds additional configurations on GetTemporaryGlueTableCredentials for Query Session Context.
* (**mediaconnect**) This release adds the DescribeSourceMetadata API. This API can be used to view the stream information of the flow's source.
* (**networkmonitor**) CloudWatch Network Monitor is a new service within CloudWatch that will help network administrators and operators continuously monitor network performance metrics such as round-trip-time and packet loss between their AWS-hosted applications and their on-premises locations.
* (**s3**) Added additional examples for some operations.
* (**secretsmanager**) Update endpoint rules and examples.

### Documentation
* (**omics**) Provides minor corrections and an updated description of APIs.

## [1.0.22] - 12/21/2023

### Features
* (**amp**) This release updates Amazon Managed Service for Prometheus APIs to support customer managed KMS keys.
* (**appintegrations**) The Amazon AppIntegrations service adds DeleteApplication API for deleting applications, and updates APIs to support third party applications reacting to workspace events and make data requests to Amazon Connect for agent and contact events.
* (**bedrockagent**) This release introduces Amazon Aurora as a vector store on Knowledge Bases for Amazon Bedrock
* (**codecommit**) AWS CodeCommit now supports customer managed keys from AWS Key Management Service. UpdateRepositoryEncryptionKey is added for updating the key configuration. CreateRepository, GetRepository, BatchGetRepositories are updated with new input or output parameters.
* (**connect**) Adds APIs to manage User Proficiencies and Predefined Attributes. Enhances StartOutboundVoiceContact API input. Introduces SearchContacts API. Enhances DescribeContact API. Adds an API to update Routing Attributes in QueuePriority and QueueTimeAdjustmentSeconds.
* (**medialive**) MediaLive now supports the ability to configure the audio that an AWS Elemental Link UHD device produces, when the device is configured as the source for a flow in AWS Elemental MediaConnect.
* (**neptunegraph**) Adds Waiters for successful creation and deletion of Graph, Graph Snapshot, Import Task and Private Endpoints for Neptune Analytics
* (**rds**) This release adds support for using RDS Data API with Aurora PostgreSQL Serverless v2 and provisioned DB clusters.
* (**rdsdata**) This release adds support for using RDS Data API with Aurora PostgreSQL Serverless v2 and provisioned DB clusters.
* (**sagemaker**) Amazon SageMaker Training now provides model training container access for debugging purposes. Amazon SageMaker Search now provides the ability to use visibility conditions to limit resource access to a single domain or multiple domains.

## [1.0.21] - 12/20/2023

### Features
* (**appstream**) This release introduces configurable clipboard, allowing admins to specify the maximum length of text that can be copied by the users from their device to the remote session and vice-versa.
* (**eks**) Add support for cluster insights, new EKS capability that surfaces potentially upgrade impacting issues.
* (**guardduty**) This release 1) introduces a new API: GetOrganizationStatistics , and 2) adds a new UsageStatisticType TOP_ACCOUNTS_BY_FEATURE for GetUsageStatistics API
* (**managedblockchainquery**) Adding Confirmation Status and Execution Status to GetTransaction Response.
* (**mediatailor**) Adds the ability to configure time shifting on MediaTailor channels using the TimeShiftConfiguration field
* (**route53**) Amazon Route 53 now supports the Canada West (Calgary) Region (ca-west-1) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.
* [#239](https://github.com/awslabs/aws-sdk-kotlin/issues/239) Generate KDoc samples from modeled examples
* Added support for [request compression](https://smithy.io/2.0/spec/behavior-traits.html#requestcompression-trait)

## [1.0.20] - 12/19/2023

### Features
* (**appsync**) This release adds additional configurations on GraphQL APIs for limits on query depth, resolver count, and introspection
* (**chimesdkmeetings**) Add meeting features to specify a maximum camera resolution, a maximum content sharing resolution, and a maximum number of attendees for a given meeting.
* (**ec2**) Provision BYOIPv4 address ranges and advertise them by specifying the network border groups option in Los Angeles, Phoenix and Dallas AWS Local Zones.
* (**fsx**) Added support for FSx for OpenZFS on-demand data replication across AWS accounts and/or regions.Added the IncludeShared attribute for DescribeSnapshots.Added the CopyStrategy attribute for OpenZFSVolumeConfiguration.
* (**marketplacecatalog**) AWS Marketplace now supports a new API, BatchDescribeEntities, which returns metadata and content for multiple entities.
* (**rds**) RDS - The release adds two new APIs: DescribeDBRecommendations and ModifyDBRecommendation

### Miscellaneous
* Disable search and address accessibility violations in documentation

## [1.0.19] - 12/18/2023

### Features
* (**cognitoidentityprovider**) Amazon Cognito now supports trigger versions that define the fields in the request sent to pre token generation Lambda triggers.
* (**eks**) Add support for EKS Cluster Access Management.
* (**route53resolver**) Add DOH protocols in resolver endpoints.

### Documentation
* (**quicksight**) A docs-only release to add missing entities to the API reference.

## [1.0.18] - 12/15/2023

### Features
* (**connect**) Adds relatedContactId field to StartOutboundVoiceContact API input. Introduces PauseContact API and ResumeContact API for Task contacts. Adds pause duration, number of pauses, timestamps for last paused and resumed events to DescribeContact API response. Adds new Rule type and new Rule action.
* (**connectcases**) Increase number of fields that can be included in CaseEventIncludedData from 50 to 200
* (**sagemaker**) This release 1) introduces a new API: DeleteCompilationJob , and 2) adds InfraCheckConfig for Create/Describe training job API

### Documentation
* (**cloud9**) Updated Cloud9 API documentation for AL2023 release
* (**kms**) Documentation updates for AWS Key Management Service
* (**rds**) Updates Amazon RDS documentation by adding code examples

## [1.0.17] - 12/14/2023

### Features
* (**appstream**) This release includes support for images of Windows Server 2022 platform.
* (**billingconductor**) Billing Conductor is releasing a new API, GetBillingGroupCostReport, which provides the ability to retrieve/view the Billing Group Cost Report broken down by attributes for a specific billing group.
* (**connect**) This release adds support for more granular billing using tags (key:value pairs)
* (**firehose**) This release, 1) adds configurable buffering hints for the Splunk destination, and 2) reduces the minimum configurable buffering interval for supported destinations
* (**gamelift**) Amazon GameLift adds the ability to add and update the game properties of active game sessions.
* (**iot**) This release adds the ability to self-manage certificate signing in AWS IoT Core fleet provisioning using the new certificate provider resource.
* (**neptunegraph**) This is the initial SDK release for Amazon Neptune Analytics
* (**opensearch**) Updating documentation for Amazon OpenSearch Service support for new zero-ETL integration with Amazon S3.
* (**quicksight**) Update Dashboard Links support; SingleAxisOptions support; Scatterplot Query limit support.

### Documentation
* (**b2bi**) Documentation updates for AWS B2B Data Interchange
* (**controltower**) Documentation updates for AWS Control Tower.
* (**workspaces**) Updated note to ensure customers understand running modes.

## [1.0.16] - 12/13/2023

### Features
* (**drs**) Adding AgentVersion to SourceServer and RecoveryInstance structures

## [1.0.15] - 12/12/2023

### Features
* (**cloudwatchlogs**) This release introduces the StartLiveTail API to tail ingested logs in near real time.
* (**imagebuilder**) This release adds the Image Workflows feature to give more flexibility and control over the image building and testing process.
* (**location**) This release 1)  adds sub-municipality field in Places API for searching and getting places information, and 2) allows optimizing route calculation based on expected arrival time.

### Fixes
* [#1008](https://github.com/awslabs/smithy-kotlin/issues/1008) Upgrade to [**smithy-kotlin** 1.0.3](https://github.com/awslabs/smithy-kotlin/releases/tag/v1.0.3) to consume fixes for URL signing special characters

## [1.0.14] - 12/11/2023

### Features
* (**neptune**) This release adds a new parameter configuration setting to the Neptune cluster related APIs that can be leveraged to switch between the underlying supported storage modes.
* (**securityhub**) Added new resource detail objects to ASFF, including resources for AwsDynamoDbTable, AwsEc2ClientVpnEndpoint, AwsMskCluster, AwsS3AccessPoint, AwsS3Bucket

### Documentation
* (**pinpoint**) This release includes Amazon Pinpoint API documentation updates pertaining to campaign message sending rate limits.

## [1.0.13] - 12/08/2023

### Features
* (**cloudwatch**) Adds support for the OpenTelemetry 1.0 output format in CloudWatch Metric Streams.
* (**ec2**) M2 Mac instances are built on Apple M2 Mac mini computers. I4i instances are powered by 3rd generation Intel Xeon Scalable processors. C7i compute optimized, M7i general purpose and R7i memory optimized instances are powered by custom 4th Generation Intel Xeon Scalable processors.
* (**finspace**) Releasing Scaling Group, Dataview, and Volume APIs

## [1.0.12] - 12/07/2023

### Features
* (**codedeploy**) This release adds support for two new CodeDeploy features: 1) zonal deployments for Amazon EC2 in-place deployments, 2) deployments triggered by Auto Scaling group termination lifecycle hook events.

## [1.0.11] - 12/06/2023

### Features
* (**backup**) AWS Backup - Features: Add VaultType to the output of DescribeRecoveryPoint, ListRecoveryPointByBackupVault API and add ResourceType to the input of ListRestoreJobs API
* (**connect**) Releasing Tagging Support for Instance Management APIS
* (**ec2**) Releasing the new cpuManufacturer attribute within the DescribeInstanceTypes API response which notifies our customers with information on who the Manufacturer is for the processor attached to the instance, for example: Intel.
* (**paymentcryptography**) AWS Payment Cryptography IPEK feature release

### Documentation
* (**comprehend**) Documentation updates for Trust and Safety features.

## [1.0.10] - 12/05/2023

### Features
* (**athena**) Adding IdentityCenter enabled request for interactive query
* (**cleanroomsml**) Updated service title from cleanroomsml to CleanRoomsML.
* (**ec2**) Adds A10G, T4G, and H100 as accelerator name options and Habana as an accelerator manufacturer option for attribute based selection

### Documentation
* (**cloudformation**) Documentation update, December 2023

## [1.0.9] - 12/04/2023

### Features
* (**billingconductor**) This release adds the ability to specify a linked account of the billing group for the custom line item resource.
* (**braket**) This release enhances service support to create quantum tasks and hybrid jobs associated with Braket Direct Reservations.
* (**cloud9**) This release adds the requirement to include the imageId parameter in the CreateEnvironmentEC2 API call.
* (**cloudformation**) Including UPDATE_* states as a success status for CreateStack waiter.
* (**finspace**) Release General Purpose type clusters
* (**medialive**) Adds support for custom color correction on channels using 3D LUT files.

### Documentation
* (**servicecatalogappregistry**) Documentation-only updates for Dawn

## [1.0.8] - 12/01/2023

### Features
* (**qconnect**) This release adds the PutFeedback API and allows providing feedback against the specified assistant for the specified target.
* (**rbin**) Added resource identifier in the output and updated error handling.
* (**verifiedpermissions**) Adds description field to PolicyStore API's and namespaces field to GetSchema.
* [#1132](https://github.com/awslabs/aws-sdk-kotlin/issues/1132) Pass client-configured LogMode through to internal credentials providers

### Miscellaneous
* Add optional `configurationSource` parameter to `ProfileCredentialsProvider`

## [1.0.7] - 11/30/2023

### Features
* (**arczonalshift**) This release adds a new capability, zonal autoshift. You can configure zonal autoshift so that AWS shifts traffic for a resource away from an Availability Zone, on your behalf, when AWS determines that there is an issue that could potentially affect customers in the Availability Zone.
* (**glue**) Adds observation and analyzer support to the GetDataQualityResult and BatchGetDataQualityResult APIs.
* (**sagemaker**) This release adds support for 1/ Code Editor, based on Code-OSS, Visual Studio Code Open Source, a new fully managed IDE option in SageMaker Studio  2/ JupyterLab, a new fully managed JupyterLab IDE experience in SageMaker Studio

## [1.0.6] - 11/30/2023

### Features
* (**marketplaceagreement**) The AWS Marketplace Agreement Service provides an API interface that helps AWS Marketplace sellers manage their agreements, including listing, filtering, and viewing details about their agreements.
* (**marketplacecatalog**) This release enhances the ListEntities API to support new entity type-specific strongly typed filters in the request and entity type-specific strongly typed summaries in the response.
* (**marketplacedeployment**) AWS Marketplace Deployment is a new service that provides essential features that facilitate the deployment of software, data, and services procured through AWS Marketplace.
* (**redshiftserverless**) This release adds the following support for Amazon Redshift Serverless: 1) cross-account cross-VPCs, 2) copying snapshots across Regions, 3) scheduling snapshot creation, and 4) restoring tables from a recovery point.

## [1.0.5] - 11/29/2023

### Features
* (**applicationautoscaling**) Amazon SageMaker customers can now use Application Auto Scaling to automatically scale the number of Inference Component copies across an endpoint to meet the varying demand of their workloads.
* (**cleanrooms**) AWS Clean Rooms now provides differential privacy to protect against user-identification attempts and machine learning modeling to allow two parties to identify similar users in their data.
* (**cleanroomsml**) Public Preview SDK release of AWS Clean Rooms ML APIs
* (**opensearch**) Launching Amazon OpenSearch Service support for new zero-ETL integration with Amazon S3. Customers can now manage their direct query data sources to Amazon S3 programatically
* (**opensearchserverless**) Amazon OpenSearch Serverless collections support an additional attribute called standby-replicas. This allows to specify whether a collection should have redundancy enabled.
* (**sagemaker**) This release adds following support 1/ Improved SDK tooling for model deployment. 2/ New Inference Component based features to lower inference costs and latency 3/ SageMaker HyperPod management. 4/ Additional parameters for FM Fine Tuning in Autopilot
* (**sagemakerruntime**) This release adds InferenceComponentName to InvokeEndpoint and InvokeEndpointWithResponseStream APIs to get inferences from the deployed InferenceComponents.

### Documentation
* (**sts**) Documentation updates for AWS Security Token Service.

## [1.0.4] - 11/28/2023

### Features
* (**accessanalyzer**) This release adds support for external access findings for S3 directory buckets to help you easily identify cross-account access. Updated service API, documentation, and paginators.
* (**bedrock**) This release adds support for customization types, model life cycle status and minor versions/aliases for model identifiers.
* (**bedrockagent**) This release introduces Agents for Amazon Bedrock
* (**bedrockagentruntime**) This release introduces Agents for Amazon Bedrock Runtime
* (**bedrockruntime**) This release adds support for minor versions/aliases for invoke model identifier.
* (**connect**) Added support for following capabilities: Amazon Connect's in-app, web, and video calling. Two-way SMS integrations. Contact Lens real-time chat analytics feature. Amazon Connect Analytics Datalake capability. Capability to configure real time chat rules.
* (**customerprofiles**) This release introduces DetectProfileObjectType API to auto generate object type mapping.
* (**qbusiness**) Amazon Q - a generative AI powered application that your employees can use to ask questions and get answers from knowledge spread across disparate content repositories, summarize reports, write articles, take actions, and much more - all within their company's connected content repositories.
* (**qconnect**) Amazon Q in Connect, an LLM-enhanced evolution of Amazon Connect Wisdom. This release adds generative AI support to Amazon Q Connect QueryAssistant and GetRecommendations APIs.
* (**s3**) Adds support for S3 Express One Zone.
* (**s3control**) Adds support for S3 Express One Zone, and InvocationSchemaVersion 2.0 for S3 Batch Operations.

## [1.0.3] - 11/28/2023

### Features
* (**elasticache**) Launching Amazon ElastiCache Serverless that enables you to create a cache in under a minute without any capacity management. ElastiCache Serverless monitors the cache's memory, CPU, and network usage and scales both vertically and horizontally to support your application's requirements.

## [1.0.2] - 11/27/2023

### Features
* (**appsync**) This update enables introspection of Aurora cluster databases using the RDS Data API
* (**b2bi**) This is the initial SDK release for AWS B2B Data Interchange.
* (**backup**) AWS Backup now supports restore testing, a new feature that allows customers to automate restore testing and validating their backups. Additionally, this release adds support for EBS Snapshots Archive tier.
* (**controltower**) This release adds the following support: 1. The EnableControl API can configure controls that are configurable.  2. The GetEnabledControl API shows the configured parameters on an enabled control. 3. The new UpdateEnabledControl API can change parameters on an enabled control.
* (**efs**) Adding support for EFS Replication to existing file system.
* (**fis**) AWS FIS adds support for multi-account experiments & empty target resolution. This release also introduces the CreateTargetAccountConfiguration API that allows experiments across multiple AWS accounts, and the ListExperimentResolvedTargets API to list target details.
* (**glue**) add observations support to DQ CodeGen config model + update document for connectiontypes supported by ConnectorData entities
* (**securityhub**) Adds and updates APIs to support central configuration. This feature allows the Security Hub delegated administrator to configure Security Hub for their entire AWS Org across multiple regions from a home Region. With this release, findings also include account name and application metadata.
* (**transcribe**) This release adds support for AWS HealthScribe APIs within Amazon Transcribe

### Documentation
* (**rds**) Updates Amazon RDS documentation for support for RDS for Db2.

## [1.0.1] - 11/27/2023

### Features
* (**accessanalyzer**) IAM Access Analyzer now continuously monitors IAM roles and users in your AWS account or organization to generate findings for unused access. Additionally, IAM Access Analyzer now provides custom policy checks to validate that IAM policies adhere to your security standards ahead of deployments.
* (**amp**) This release adds support for the Amazon Managed Service for Prometheus collector, a fully managed, agentless Prometheus metrics scraping capability.
* (**bcmdataexports**) Users can create, read, update, delete Exports of billing and cost management data.  Users can get details of Export Executions and details of Tables for exporting.  Tagging support is provided for Exports
* (**cloudtrail**) CloudTrail Lake now supports federating event data stores. giving users the ability to run queries against their event data using Amazon Athena.
* (**cloudwatchlogs**) Added APIs to Create, Update, Get, List and Delete LogAnomalyDetectors and List and Update Anomalies in Detector. Added LogGroupClass attribute for LogGroups to classify loggroup as Standard loggroup with all capabilities or InfrequentAccess loggroup with limited capabilities.
* (**codestarconnections**) This release adds support for the CloudFormation Git sync feature. Git sync enables updating a CloudFormation stack from a template stored in a Git repository.
* (**computeoptimizer**) This release enables AWS Compute Optimizer to analyze and generate recommendations with customization and discounts preferences.
* (**configservice**) Support Periodic Recording for Configuration Recorder
* (**controltower**) Add APIs to create and manage a landing zone.
* (**costoptimizationhub**) This release launches Cost Optimization Hub, a new AWS Billing and Cost Management feature that helps you consolidate and prioritize cost optimization recommendations across your AWS Organizations member accounts and AWS Regions, so that you can get the most out of your AWS spend.
* (**detective**) Added new APIs in Detective to support resource investigations
* (**ecs**) Adds a new 'type' property to the Setting structure. Adds a new AccountSetting - guardDutyActivate for ECS.
* (**efs**) Adding support for EFS Archive lifecycle configuration.
* (**eks**) This release adds support for EKS Pod Identity feature. EKS Pod Identity makes it easy for customers to obtain IAM permissions for the applications running in their EKS clusters.
* (**eksauth**) This release adds support for EKS Pod Identity feature. EKS Pod Identity makes it easy for customers to obtain IAM permissions for their applications running in the EKS clusters.
* (**elasticloadbalancingv2**) This release enables both mutual authentication (mTLS), and Automatic Target Weights (ATW) for Application Load Balancers.
* (**freetier**) This is the initial SDK release for the AWS Free Tier GetFreeTierUsage API
* (**fsx**) Added support for FSx for ONTAP scale-out file systems and FlexGroup volumes. Added the HAPairs field and ThroughputCapacityPerHAPair for filesystem. Added AggregateConfiguration (containing Aggregates and ConstituentsPerAggregate) and SizeInBytes for volume.
* (**guardduty**) Add support for Runtime Monitoring for ECS and ECS-EC2.
* (**iotfleetwise**) AWS IoT FleetWise introduces new APIs for vision system data, such as data collected from cameras, radars, and lidars. You can now model and decode complex data types.
* (**lakeformation**) This release adds four new APIs "DescribeLakeFormationIdentityCenterConfiguration", "CreateLakeFormationIdentityCenterConfiguration", "DescribeLakeFormationIdentityCenterConfiguration", and "DeleteLakeFormationIdentityCenterConfiguration", and also updates the corresponding documentation.
* (**lexmodelsv2**) This release introduces new generative AI features in AWS Lex: Assisted Slot Resolution, Descriptive Bot Building, and Sample Utterance Generation. These features leverage large language models available through Amazon Bedrock to improve the bot builder and customer experiences.
* (**lexruntimev2**) This release introduces support for interpretationSource in the runtime service response.
* (**managedblockchain**) Add optional NetworkType property to Accessor APIs
* (**personalize**) Enables metadata in recommendations, recommendations with themes, and next best action recommendations
* (**personalizeevents**) This release enables PutActions and PutActionInteractions
* (**personalizeruntime**) Enables metadata in recommendations and next best action recommendations
* (**quicksight**) This release launches new APIs for trusted identity propagation setup and supports creating datasources using trusted identity propagation as authentication method for QuickSight accounts configured with IAM Identity Center.
* (**redshift**) This release adds support for multi-data warehouse writes through data sharing.
* (**repostspace**) Initial release of AWS re:Post Private
* (**s3**) Adding new params - Key and Prefix, to S3 API operations for supporting S3 Access Grants. Note - These updates will not change any of the existing S3 API functionality.
* (**s3control**) Introduce Amazon S3 Access Grants, a new S3 access control feature that maps identities in directories such as Active Directory, or AWS Identity and Access Management (IAM) Principals, to datasets in S3.
* (**secretsmanager**) AWS Secrets Manager has released the BatchGetSecretValue API, which allows customers to fetch up to 20 Secrets with a single request using a list of secret names or filters.
* (**securityhub**) Adds and updates APIs to support customizable security controls. This feature allows Security Hub customers to provide custom parameters for security controls. With this release, findings for controls that support custom parameters will include the parameters used to generate the findings.
* (**sfn**) Adds new TestState operation which accepts the definition of a single state and executes it. You can test a state without creating a state machine or updating an existing state machine.
* (**transcribe**) This release adds support for transcriptions from audio sources in 64 new languages and introduces generative call summarization in Transcribe Call Analytics (Post call)
* (**workspaces**) The release introduces Multi-Region Resilience one-way data replication that allows you to replicate data from your primary WorkSpace to a standby WorkSpace in another AWS Region. DescribeWorkspaces now returns the status of data replication.
* (**workspacesthinclient**) Initial release of Amazon WorkSpaces Thin Client

## [1.0.0] - 11/26/2023

### Features
* [#659](https://github.com/awslabs/smithy-kotlin/issues/659) BREAKING: Overhaul URL APIs to clarify content encoding, when data is in which state, and to reduce the number of times data is encoded/decoded

### Fixes
* **Breaking**: Make some properties of IoT types optional. Previously they defaulted to false, which isn't what the service expects.

## [0.36.1-beta] - 11/22/2023

### Features
* (**kinesis**) This release adds support for resource based policies on streams and consumers.
* (**s3control**) Amazon S3 Batch Operations now manages buckets or prefixes in a single step.
* (**sagemaker**) This feature adds the end user license agreement status as a model access configuration parameter.

## [0.36.0-beta] - 11/21/2023

### Features
* (**cloudfront**) This release adds support for CloudFront KeyValueStore, a globally managed key value datastore associated with CloudFront Functions.
* (**cloudfrontkeyvaluestore**) This release adds support for CloudFront KeyValueStore, a globally managed key value datastore associated with CloudFront Functions.
* (**inspectorscan**) This release adds support for the new Amazon Inspector Scan API. The new Inspector Scan API can synchronously scan SBOMs adhering to the CycloneDX v1.5 format.
* (**iotsitewise**) Adds 1/ user-defined unique identifier for asset and model metadata, 2/ asset model components, and 3/ query API for asset metadata and telemetry data. Supports 4/ multi variate anomaly detection using Amazon Lookout for Equipment, 5/ warm storage tier, and 6/ buffered ingestion of time series data.
* (**iottwinmaker**) This release adds following support. 1. New APIs for metadata bulk operations. 2. Modify the component type API to support composite component types - nesting component types within one another. 3. New list APIs for components and properties. 4. Support the larger scope digital twin modeling.
* (**s3**) Add support for automatic date based partitioning in S3 Server Access Logs.
* [#1002](https://github.com/awslabs/aws-sdk-kotlin/issues/1002), [#1003](https://github.com/awslabs/aws-sdk-kotlin/issues/1003) Make region providers public and allow profile name override
* [#1055](https://github.com/awslabs/aws-sdk-kotlin/issues/1055) **BREAKING**: Make the AWS retry policy inheritable
* [#199](https://github.com/awslabs/aws-sdk-kotlin/issues/199) Handle S3 errors returned with an HTTP 200 response

### Fixes
* **Breaking**: Make properties of S3Control PublicAccessBlockConfiguration optional by removing default values. Previously they defaulted to false, which caused invalid requests

### Documentation
* (**ec2**) Documentation updates for Amazon EC2.

## [0.35.1-beta] - 11/20/2023

### Features
* (**codestarconnections**) This release updates a few CodeStar Connections related APIs.
* (**docdb**) Amazon DocumentDB updates for new cluster storage configuration: Amazon DocumentDB I/O-Optimized.
* (**ec2**) This release adds support for Security group referencing over Transit gateways, enabling you to simplify Security group management and control of instance-to-instance traffic across VPCs that are connected by Transit gateway.

## [0.35.0-beta] - 11/17/2023

### Features
* (**appmesh**) Change the default value of these fields from 0 to null: MaxConnections, MaxPendingRequests, MaxRequests, HealthCheckThreshold, PortNumber, and HealthCheckPolicy -> port. Users are not expected to perceive the change, except that badRequestException is thrown when required fields missing configured.
* (**athena**) Adding SerivicePreProcessing time metric
* (**cloudformation**) This release adds a new flag ImportExistingResources to CreateChangeSet. Specify this parameter on a CREATE- or UPDATE-type change set to import existing resources with custom names instead of recreating them.
* (**codepipeline**) CodePipeline now supports overriding source revisions to achieve manual re-deploy of a past revision
* (**codestarconnections**) This release adds support for the CloudFormation Git sync feature. Git sync enables updating a CloudFormation stack from a template stored in a Git repository.
* (**connect**) This release adds WISDOM_QUICK_RESPONSES as new IntegrationType of Connect IntegrationAssociation resource and bug fixes.
* (**ec2**) This release adds new features for Amazon VPC IP Address Manager (IPAM) Allowing a choice between Free and Advanced Tiers, viewing public IP address insights across regions and in Amazon Cloudwatch, use IPAM to plan your subnet IPs within a VPC and bring your own autonomous system number to IPAM.
* (**ecr**) Documentation and operational updates for Amazon ECR, adding support for pull through cache rules for upstream registries that require authentication.
* (**emr**) Launch support for IAM Identity Center Trusted Identity Propagation and workspace storage encryption using AWS KMS in EMR Studio
* (**eventbridge**) Introduces a new rule state ENABLED_WITH_ALL_CLOUDTRAIL_MANAGEMENT_EVENTS for matching with Get, List and Describe AWS API call events from CloudTrail.
* (**internetmonitor**) Adds new querying capabilities for running data queries on a monitor
* (**ivs**) type & defaulting refinement to various range properties
* (**ivschat**) type & defaulting refinement to various range properties
* (**location**) Remove default value and allow nullable for request parameters having minimum value larger than zero.
* (**medialive**) MediaLive has now added support for per-output static image overlay.
* (**mgn**) Removed invalid and unnecessary default values.
* (**osis**) Add support for enabling a persistent buffer when creating or updating an OpenSearch Ingestion pipeline. Add tags to Pipeline and PipelineSummary response models.
* (**pipes**) TargetParameters now properly supports BatchJobParameters.ArrayProperties.Size and BatchJobParameters.RetryStrategy.Attempts being optional, and EcsTaskParameters.Overrides.EphemeralStorage.SizeInGiB now properly required when setting EphemeralStorage
* (**rds**) This release adds support for option groups and replica enhancements to Amazon RDS Custom.
* (**redshift**) Updated SDK for Amazon Redshift, which you can use to configure a connection with IAM Identity Center to manage access to databases. With these, you can create a connection through a managed application. You can also change a managed application, delete it, or get information about an existing one.
* (**redshiftserverless**) Updated SDK for Amazon Redshift Serverless, which provides the ability to configure a connection with IAM Identity Center to manage user and group access to databases.
* (**s3**) Removes all default 0 values for numbers and false values for booleans
* (**ssoadmin**) Improves support for configuring RefreshToken and TokenExchange grants on applications.
* (**ssooidc**) Adding support for `sso-oauth:CreateTokenWithIAM`.
* (**trustedadvisor**) AWS Trusted Advisor introduces new APIs to enable you to programmatically access Trusted Advisor best practice checks, recommendations, and prioritized recommendations. Trusted Advisor APIs enable you to integrate Trusted Advisor with your operational tools to automate your workloads.
* (**verifiedpermissions**) Adding BatchIsAuthorized API which supports multiple authorization requests against a PolicyStore
* (**wisdom**) This release adds QuickResponse as a new Wisdom resource and Wisdom APIs for import, create, read, search, update and delete QuickResponse resources.
* ⚠️ **IMPORTANT**: Enable account ID based endpoint routing for services that support it

### Fixes
* **Breaking**: Make some types for various services optional by removing default values

### Documentation
* (**cloud9**) A minor doc only update related to changing the date of an API change.
* (**dlm**) Added support for SAP HANA in Amazon Data Lifecycle Manager EBS snapshot lifecycle policies with pre and post scripts.
* (**kinesisvideo**) Docs only build to bring up-to-date with public docs.
* (**sts**) API updates for the AWS Security Token Service

### Miscellaneous
* Upgrade dependencies to their latest versions, notably Kotlin 1.9.20
* Remove Macie v1 service

## [0.34.9-beta] - 11/16/2023

### Features
* (**codecatalyst**) This release includes updates to the Dev Environment APIs to include an optional vpcConnectionName parameter that supports using Dev Environments with Amazon VPC.
* (**dlm**) This release adds support for Amazon Data Lifecycle Manager default policies for EBS snapshots and EBS-backed AMIs.
* (**ec2**) Enable use of tenant-specific PublicSigningKeyUrl from device trust providers and onboard jumpcloud as a new device trust provider.
* (**fsx**) Enables customers to update their PerUnitStorageThroughput on their Lustre file systems.
* (**glue**) Introduces new column statistics APIs to support statistics generation for tables within the Glue Data Catalog.
* (**imagebuilder**) This release adds the Image Lifecycle Management feature to automate the process of deprecating, disabling and deleting outdated images and their associated resources.
* (**iot**) GA release the ability to index and search devices based on their GeoLocation data. With GeoQueries you can narrow your search to retrieve devices located in the desired geographic boundary.
* (**ivsrealtime**) This release introduces server side composition and recording for stages.
* (**kafka**) Added a new API response field which determines if there is an action required from the customer regarding their cluster.
* (**lambda**) Adds support for logging configuration in Lambda Functions. Customers will have more control how their function logs are captured and to which cloud watch log group they are delivered also.
* (**macie2**) This release adds support for configuring Macie to assume an IAM role when retrieving sample occurrences of sensitive data reported by findings.
* (**mediapackage**) DRM_TOP_LEVEL_COMPACT allows placing content protection elements at the MPD level and referenced at the AdaptationSet level
* (**pinpointsmsvoicev2**) Amazon Pinpoint now offers additional operations as part of version 2 of the SMS and voice APIs. This release includes 26 new APIs to create and manage phone number registrations, add verified destination numbers, and request sender IDs.
* (**polly**) Add new engine - long-form - dedicated for longer content, such as news articles, training materials, or marketing videos.
* (**quicksight**) Custom permission support for QuickSight roles; Three new datasources STARBURST, TRINO, BIGQUERY; Lenient mode changes the default behavior to allow for exporting and importing with certain UI allowed errors, Support for permissions and tags export and import.
* (**sagemaker**) Amazon SageMaker Studio now supports Trainium instance types - trn1.2xlarge, trn1.32xlarge, trn1n.32xlarge.
* (**ssm**) This release introduces the ability to filter automation execution steps which have parent steps. In addition, runbook variable information is returned by GetAutomationExecution and parent step information is returned by the DescribeAutomationStepExecutions API.
* (**ssmincidents**) Introduces new APIs ListIncidentFindings and BatchGetIncidentFindings to use findings related to an incident.
* (**ssoadmin**) Instances bound to a single AWS account, API operations for managing instances and applications, and assignments to applications are now supported. Trusted identity propagation is also supported, with new API operations for managing trusted token issuers and application grants and scopes.
* (**transfer**) Introduced S3StorageOptions for servers to enable directory listing optimizations and added Type fields to logical directory mappings.

## [0.34.8-beta] - 11/15/2023

### Features
* (**autoscaling**) This release introduces Instance Maintenance Policy, a new EC2 Auto Scaling capability that allows customers to define whether instances are launched before or after existing instances are terminated during instance replacement operations.
* (**cloudtrail**) The Lake Repricing feature lets customers configure a BillingMode for an event data store. The BillingMode determines the cost for ingesting and storing events and the default and maximum retention period for the event data store.
* (**codecatalyst**) This release adds functionality for retrieving information about workflows and workflow runs and starting workflow runs in Amazon CodeCatalyst.
* (**ec2**) AWS EBS now supports Snapshot Lock, giving users the ability to lock an EBS Snapshot to prohibit deletion of the snapshot. This release introduces the LockSnapshot, UnlockSnapshot & DescribeLockedSnapshots APIs to manage lock configuration for snapshots. The release also includes the dl2q_24xlarge.
* (**finspace**) Adding deprecated trait on Dataset Browser Environment APIs
* (**finspacedata**) Adding deprecated trait to APIs in this name space.
* (**lambda**) Add Java 21 (java21) support to AWS Lambda
* (**mwaa**) This Amazon MWAA release adds support for customer-managed VPC endpoints. This lets you choose whether to create, and manage your environment's VPC endpoints, or to have Amazon MWAA create, and manage them for you.
* (**redshift**) The custom domain name SDK for Amazon Redshift provisioned clusters is updated with additional required parameters for modify and delete operations. Additionally, users can provide domain names with longer top-level domains.
* (**s3control**) Add 5 APIs to create, update, get, list, delete S3 Storage Lens group(eg. CreateStorageLensGroup), 3 APIs for tagging(TagResource,UntagResource,ListTagsForResource), and update to StorageLensConfiguration to allow metrics to be aggregated on Storage Lens groups.
* (**ssmsap**) Update the default value of MaxResult to 50.

### Documentation
* (**rds**) Updates Amazon RDS documentation for support for upgrading RDS for MySQL snapshots from version 5.7 to version 8.0.

## [0.34.7-beta] - 11/14/2023

### Features
* (**backup**) AWS Backup - Features: Provide Job Summary for your backup activity.
* (**cleanrooms**) This feature provides the ability for the collaboration creator to configure either the member who can run queries or a different member in the collaboration to be billed for query compute costs.
* (**connect**) Introducing SegmentAttributes parameter for StartChatContact API
* (**glue**) Introduces new storage optimization APIs to support automatic compaction of Apache Iceberg tables.
* (**iot**) This release introduces new attributes in API CreateSecurityProfile, UpdateSecurityProfile and DescribeSecurityProfile to support management of Metrics Export for AWS IoT Device Defender Detect.
* (**lambda**) Add Python 3.12 (python3.12) support to AWS Lambda
* (**mediatailor**) Removed unnecessary default values.
* (**pipes**) Added support (via new LogConfiguration field in CreatePipe and UpdatePipe APIs) for logging to Amazon CloudWatch Logs, Amazon Simple Storage Service (Amazon S3), and Amazon Kinesis Data Firehose
* (**resourceexplorer2**) Resource Explorer supports multi-account search. You can now use Resource Explorer to search and discover resources across AWS accounts within your organization or organizational unit.
* (**sagemaker**) This release makes Model Registry Inference Specification fields as not required.
* (**sfn**) This release adds support to redrive executions in AWS Step Functions with a new RedriveExecution operation.
* Enable auth scheme resolution via endpoints for S3 and EventBridge

### Documentation
* (**signer**) Documentation updates for AWS Signer

## [0.34.6-beta] - 11/13/2023

### Features
* (**databasemigrationservice**) Added new Db2 LUW Target endpoint with related endpoint settings. New executeTimeout endpoint setting for mysql endpoint. New ReplicationDeprovisionTime field for serverless describe-replications.
* (**dataexchange**) Removed Required trait for DataSet.OriginDetails.ProductId.
* (**ec2**) Adds the new EC2 DescribeInstanceTopology API, which you can use to retrieve the network topology of your running instances on select platform types to determine their relative proximity to each other.
* (**ecs**) Adds a Client Token parameter to the ECS RunTask API. The Client Token parameter allows for idempotent RunTask requests.
* (**emr**) Updated GetClusterSessionCredentials API  to allow Amazon SageMaker Studio to connect to EMR on EC2 clusters to support IdentityCenter/PEZ integration.
* (**servicecatalogappregistry**) When the customer associates a resource collection to their application with this new feature, then a new application tag will be applied to all supported resources that are part of that collection. This allows customers to more easily find the application that is associated with those resources.
* (**transcribestreaming**) This release enables customers to call the AWS Transcribe streaming service with the capability of identifying multiple languages in the stream.

## [0.34.5-beta] - 11/10/2023

### Features
* (**controltower**) AWS Control Tower supports tagging for enabled controls. This release introduces TagResource, UntagResource and ListTagsForResource APIs to manage tags in existing enabled controls. It updates EnabledControl API to tag resources at creation time.
* (**costandusagereportservice**) This release adds support for tagging and customers can now tag report definitions. Additionally, ReportStatus is now added to report definition to show when the last delivered time stamp and if it succeeded or not.
* (**ec2**) EC2 adds API updates to enable ENA Express at instance launch time.
* (**marketplaceentitlementservice**) Add paginators to GetEntitlements.
* (**mediaconvert**) This release includes the ability to specify any input source as the primary input for corresponding follow modes, and allows users to specify fit and fill behaviors without resizing content.

### Documentation
* (**fms**) Adds optimizeUnassociatedWebACL flag to ManagedServiceData, updates third-party firewall examples, and other minor documentation updates.
* (**rds**) Updates Amazon RDS documentation for zero-ETL integrations.

## [0.34.4-beta] - 11/09/2023

### Features
* (**cloudformation**) Added new ConcurrencyMode feature for AWS CloudFormation StackSets for faster deployments to target accounts.
* (**cloudtrail**) The Insights in Lake feature lets customers enable CloudTrail Insights on a source CloudTrail Lake event data store and create a destination event data store to collect Insights events based on unusual management event activity in the source event data store.
* (**cloudwatchlogs**) Update to support new APIs for delivery of logs from AWS services.
* (**comprehend**) This release adds support for toxicity detection and prompt safety classification.
* (**connect**) This release adds the ability to integrate customer lambda functions with Connect attachments for scanning and updates the ListIntegrationAssociations API to support filtering on IntegrationArn.
* (**ec2**) AWS EBS now supports Block Public Access for EBS Snapshots. This release introduces the EnableSnapshotBlockPublicAccess, DisableSnapshotBlockPublicAccess and GetSnapshotBlockPublicAccessState APIs to manage account-level public access settings for EBS Snapshots in an AWS Region.
* (**eks**) Adding EKS Anywhere subscription related operations.
* (**lambda**) Add Custom runtime on Amazon Linux 2023 (provided.al2023) support to AWS Lambda.
* (**omics**) Support UBAM filetype for Omics Storage and make referenceArn optional

## [0.34.3-beta] - 11/09/2023

### Features
* (**sqs**) This release enables customers to call SQS using AWS JSON-1.0 protocol and bug fix.

## [0.34.2-beta] - 11/08/2023

### Features
* (**connect**) This release clarifies in our public documentation that InstanceId is a requirement for SearchUsers API requests.
* (**connectcases**) This release adds the ability to add/view comment authors through CreateRelatedItem and SearchRelatedItems API. For more information see https://docs.aws.amazon.com/cases/latest/APIReference/Welcome.html
* (**datasync**) This change allows for 0 length access keys and secret keys for object storage locations. Users can now pass in empty string credentials.
* (**guardduty**) Added API support for new GuardDuty EKS Audit Log finding types.
* (**lambda**) Add Node 20 (nodejs20.x) support to AWS Lambda.
* (**lexmodelsv2**) AWS Lex now supports selective log capture in conversation logs. When you enable this option within the conversation log settings, only the utterances that trigger intents and slots specified in session attributes will be logged.
* (**omics**) Adding Run UUID and Run Output URI: GetRun and StartRun API response has two new fields "uuid" and "runOutputUri".
* (**redshiftserverless**) Added a new parameter in the workgroup that helps you control your cost for compute resources. This feature provides a ceiling for RPUs that Amazon Redshift Serverless can scale up to. When automatic compute scaling is required, having a higher value for MaxRPU can enhance query throughput.
* (**resiliencehub**) AWS Resilience Hub enhances Resiliency Score, providing actionable recommendations to improve application resilience. Amazon Elastic Kubernetes Service (EKS) operational recommendations have been added to help improve the resilience posture of your applications.
* (**sqs**) This release enables customers to call SQS using AWS JSON-1.0 protocol.

### Documentation
* (**rds**) This Amazon RDS release adds support for patching the OS of an RDS Custom for Oracle DB instance. You can now upgrade the database or operating system using the modify-db-instance command.

## [0.34.1-beta] - 11/07/2023

### Features
* (**dataexchange**) Updated SendDataSetNotificationRequest Comment to be maximum length 4096.
* (**lifecycle**) Added support for pre and post scripts in Amazon Data Lifecycle Manager EBS snapshot lifecycle policies.
* (**rds**) This Amazon RDS release adds support for the multi-tenant configuration. In this configuration, an RDS DB instance can contain multiple tenant databases. In RDS for Oracle, a tenant database is a pluggable database (PDB).

## [0.34.0-beta] - 11/06/2023

### Features
* (**awslily**) Added new API that allows Amazon Connect Outbound Campaigns to create contacts in Amazon Connect when ingesting your dial requests.
* (**codebuild**) AWS CodeBuild now supports AWS Lambda compute.
* (**docdb**) Update the input of CreateDBInstance and ModifyDBInstance to support setting CA Certificates. Update the output of DescribeDBInstance and DescribeDBEngineVersions to show current and supported CA certificates.
* (**iam**) Add partitional endpoint for iso-e.
* (**iis**) This release extends the GetReservationPurchaseRecommendation API to support recommendations for Amazon MemoryDB reservations.
* (**mwaa**) This release adds support for Apache Airflow version 2.7.2. This version release includes support for deferrable operators and triggers.
* (**polly**) Amazon Polly adds new US English voices - Danielle and Gregory. Danielle and Gregory are available as Neural voices only.
* (**route53**) Add partitional endpoints for iso-e and iso-f.
* **BREAKING**: Remove operations/fields which were marked deprecated before 11/28/2023. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/1103) for more details.

### Fixes
* [#1101](https://github.com/awslabs/aws-sdk-kotlin/issues/1101) Fix endpoint builtin bound to wrong config parameter

## [0.33.1-beta] - 11/01/2023

### Features
* Support EKS endpoints and auth token file in container credentials provider.

### Fixes
* [#1098](https://github.com/awslabs/aws-sdk-kotlin/issues/1098) Add `forcePathStyle` and `enableAccelerate` config properties back to the S3 client.

### Miscellaneous
* Upgrade to the latest version of **smithy-kotlin**

## [0.33.0-beta] - 10/26/2023

### Features
* **BREAKING**: Update codegen to improve nullability of generated types.
* [#605](https://github.com/awslabs/aws-sdk-kotlin/issues/605), [#805](https://github.com/awslabs/aws-sdk-kotlin/issues/805) Publish a BOM and a Version Catalog
* Detect and automatically correct clock skew to prevent signing errors

### Fixes
* Ignore empty environment variable and system property strings when evaluating AWS credentials

### Miscellaneous
* Upgrade Kotlin to 1.9.10
* Sync AWS models and upgrade smithy-kotlin
* Upgrade Dokka to 1.9.0
* **Breaking** Removed `enableAccelerate` & `forcePathStyle` from S3 config. As well as `use_accelerate_endpoint` & `addressing_style` from AWS profile configuration
* **BREAKING**: Remove `smithy.client.request.size`, `smithy.client.response.size`, `smithy.client.retries` metrics. Rename all `smithy.client.*` metrics to `smithy.client.call.*`.
* Remove GameSparks service
* Add skeleton implementation of a second KMP target
* Added `s3_use_arn_region` & `s3_disable_multiregion_access_points` to AWS profile configuration

## [0.32.5-beta] - 10/12/2023

### Features
* [#945](https://github.com/awslabs/aws-sdk-kotlin/issues/945) Add new sources for User-Agent app id

### Miscellaneous
* Sync to the latest versions of **smithy-kotlin** and AWS service models

## [0.32.4-beta] - 10/06/2023

### Miscellaneous
* Track upstream changes that make `ByteArrayContent` and friends internal. Users should only be using `ByteStream.fromBytes()`, `ByteStream.fromString()`, and `HttpBody.fromBytes()`.

## [0.32.3-beta] - 09/28/2023

### Fixes
* [#1048](https://github.com/awslabs/aws-sdk-kotlin/issues/1048) Restore public constructor for `EcsCredentialsProvider`
* [#1044](https://github.com/awslabs/aws-sdk-kotlin/issues/1044) ignore `__type` when deserializing union for AWS JSON 1.0, AWS JSON 1.1, and AWS restJson 1

### Miscellaneous
* Generate internal-only clients with `internal` visibility
* sync AWS models and upgrade smithy kotlin

## [0.32.2-beta] - 09/15/2023

### Miscellaneous
* [#946](https://github.com/awslabs/aws-sdk-kotlin/issues/946) Refactor CredentialsProvider APIs
* Sync smithy-kotlin and AWS service models.

## [0.32.1-beta] - 09/08/2023

### Features
* [#1033](https://github.com/awslabs/aws-sdk-kotlin/issues/1033) Add `SystemPropertyCredentialsProvider` and make it first in default chain credentials provider
* Allow endpoint URL configuration via env and shared config.
* [#1000](https://github.com/awslabs/aws-sdk-kotlin/issues/1000) Add more parameters for fetching STS credentials

### Fixes
* [#935](https://github.com/awslabs/smithy-kotlin/issues/935) Fix closing an event stream causing an IllegalStateException

### Miscellaneous
* Sync AWS models and bump smithy-kotlin

## [0.32.0-beta] - 08/31/2023

### Miscellaneous
* **BREAKING**: Refactor HttpCall and HttpResponse types
* Bump **smithy-kotlin** and AWS service models to latest versions

## [0.31.0-beta] - 08/24/2023

### Features
* Support initial-request and initial-response for event streams using RPC-based protocols

### Fixes
* [#1029](https://github.com/awslabs/aws-sdk-kotlin/issues/1029) Update smithy-kotlin to 0.26.0

### Miscellaneous
* **BREAKING**: prefix generated endpoint and auth scheme providers with client name and track upstream changes
* Sync AWS models
* Refactor ClientOption to AttributeKey directly and track upstream HttpContext changes

## [0.30.1-beta] - 08/17/2023

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.30.0-beta] - 08/11/2023

### Features
* [#583](https://github.com/awslabs/aws-sdk-kotlin/issues/583) Make user-supplied region available to config resolution providers
* [#1004](https://github.com/awslabs/aws-sdk-kotlin/issues/1004) Make RegionProviderChain accept a list of RegionProvider

### Fixes
* [#194](https://github.com/awslabs/aws-sdk-kotlin/issues/194) Correctly parse and handle `GetBucketLocation` responses

### Miscellaneous
* Upgrade Kotlin to 1.8.22
* [#968](https://github.com/awslabs/aws-sdk-kotlin/issues/968) Add service-level benchmarks
* Upgrade kotlinx.coroutines to 1.7.3
* Sync AWS service models and **smithy-kotlin** to latest versions

## [0.29.1-beta] - 07/27/2023

### Features
* [#745](https://github.com/awslabs/aws-sdk-kotlin/issues/745) Validate returned content length on S3 `GetObject` responses.

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.29.0-beta] - 07/20/2023

### Features
* [#146](https://github.com/awslabs/smithy-kotlin/issues/146), [#800](https://github.com/awslabs/aws-sdk-kotlin/issues/800) Enable **Timestream Query** and **Timestream Write** service clients
* [#969](https://github.com/awslabs/aws-sdk-kotlin/issues/969) Make `region` an optional client config parameter to support multi-region use cases

### Miscellaneous
* **BREAKING**: Refactor observability API and configuration. See the [discussion](https://github.com/awslabs/aws-sdk-kotlin/discussions/981) for more information.
* Sync AWS service models.

## [0.28.2-beta] - 07/13/2023

### Fixes
* [#242](https://github.com/awslabs/aws-sdk-kotlin/issues/242) Correctly handle and throw `InvalidChangeBatch` responses from Route53

### Miscellaneous
* Sync AWS service models

## [0.28.1-beta] - 07/06/2023

### Miscellaneous
* Upgrade smithy-kotlin and sync service models.

## [0.28.0-beta] - 06/29/2023

### Features
* [#701](https://github.com/awslabs/aws-sdk-kotlin/issues/701) **Breaking**: Simplify mechanisms for setting/updating retry strategies in client config. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/964) for more details.
* [#701](https://github.com/awslabs/aws-sdk-kotlin/issues/701) Add adaptive retry mode

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.27.2-beta] - 06/22/2023

### Miscellaneous
* Sync AWS service models

## [0.27.1-beta] - 06/19/2023

### Fixes
* [#815](https://github.com/awslabs/aws-sdk-kotlin/issues/815) Fix a bug in forming S3 WriteGetObjectResponse's host path

### Miscellaneous
* Sync AWS service models.
* Update user agent header to new cross-SDK format

## [0.27.0-beta] - 06/09/2023

### Miscellaneous
* Upgrade smithy-kotlin and sync service models.
* [#824](https://github.com/awslabs/aws-sdk-kotlin/issues/824) **BREAKING:** Update closeability of various types of CredentialsProvider

## [0.26.1-beta] - 06/01/2023

### Fixes
* Fix infinite pagination in S3 `ListParts`

### Miscellaneous
* Sync AWS models and bump smithy-kotlin

## [0.26.0-beta] - 05/25/2023

### Features
* [#755](https://github.com/awslabs/smithy-kotlin/issues/755) **Breaking**: Refresh presigning APIs to simplify usage and add new capabilities. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/925) for more information.

### Miscellaneous
* Sync the latest versions of AWS service models and **smithy-kotlin**

## [0.25.0-beta] - 05/19/2023

### Features
* **Breaking**: Make HTTP engines configurable in client config during initialization and during `withCopy`. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/new?category=announcements) for more information.

### Miscellaneous
* Sync smithy-kotlin and AWS service models.

## [0.24.0-beta] - 05/12/2023

### Features
* [#806](https://github.com/awslabs/aws-sdk-kotlin/issues/806) Add support for IAM Identity Center authentication and sso-session support in shared config

### Miscellaneous
* **BREAKING**: Refactor `SsoCredentialsProvider` to take an `SsoSession` parameter.
* Sync smithy-kotlin and service models.

## [0.23.0-beta] - 05/04/2023

### Features
* [#432](https://github.com/awslabs/aws-sdk-kotlin/issues/432) Enable resolving `LogMode` from environment

### Miscellaneous
* Sync AWS models and **smithy-kotlin** to the latest versions
* Refactor environment settings and retry modes into smithy-kotlin
* Sync AWS models and bump smithy-kotlin version

## [0.22.1-beta] - 04/21/2023

### Features
* BREAKING: Add support for retrying certain transient HTTP errors. `RetryErrorType.Timeout` was renamed to `RetryErrorType.Transient`.

### Miscellaneous
* Refactor internal endpoint resolver execution to track upstream changes.
* Sync AWS models and bump smithy-kotlin version

## [0.22.0-beta] - 04/14/2023

### Miscellaneous
* Refactor identity and authentication APIs
* Upgrade smithy-kotlin and sync AWS service models.

## [0.21.5-beta] - 04/06/2023

### Fixes
* [#492](https://github.com/awslabs/aws-sdk-kotlin/issues/492) Don't use potentially stale profile when retrieving credentials via IMDS.

### Miscellaneous
* Upgrade **smithy-kotlin** to 0.16.6
* Sync AWS models to latest versions
* Upgrade smithy to pull in protocol tests for intEnum support.

## [0.21.4-beta] - 03/30/2023

### Features
* Add support for awsQuery-compatible error responses.

### Miscellaneous
* add clarifying docs for endpointUrl
* Sync latest AWS models

## [0.21.3-beta] - 03/16/2023

### Features
* [#206](https://github.com/awslabs/aws-sdk-kotlin/issues/206) Add support for loading FIPS and dual-stack endpoint settings from env, system properties, and shared config.
* [#206](https://github.com/awslabs/aws-sdk-kotlin/issues/206) Add support for loading S3 accelerate and addressing settings from shared config.

### Fixes
* [#874](https://github.com/awslabs/aws-sdk-kotlin/issues/874) Ensure all unsigned operations are accessible without credentials in CognitoIdentityProvider.

### Miscellaneous
* Update smithy-kotlin version and sync service models.

## [0.21.2-beta] - 03/09/2023

### Features
* Add sub-property support for AWS config.

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.21.1-beta] - 03/02/2023

### Miscellaneous
* Sync AWS service models and **smithy-kotlin** version

## [0.21.0-beta] - 02/24/2023

### Features
* Sync AWS service models and **smithy-kotlin** version

### Miscellaneous
* Refactor: move CachedCredentialsProvider and CredentialsProviderChain to smithy-kotlin under aws.smithy.kotlin.runtime.auth.awscredentials package

## [0.20.3-beta] - 02/16/2023

### Features
* [#839](https://github.com/awslabs/aws-sdk-kotlin/issues/839) Add `Expect: 100-continue` header to S3 PUT requests over 2MB

### Miscellaneous
* Upgrade smithy-kotlin and sync latest service models.

## [0.20.2-beta] - 02/09/2023

### Features
* Add configuration for retry policy on clients

### Fixes
* [#836](https://github.com/awslabs/aws-sdk-kotlin/issues/836) Fix bug caused by reading too few bytes when parsing header values in event streams

### Miscellaneous
* Sync AWS service models
* Refactor: track upstream module changes
* Refactor: track upstream HTTP module changes

## [0.20.1-beta] - 02/06/2023

### Features
* Sync to latest AWS service models
* [#446](https://github.com/awslabs/smithy-kotlin/issues/446) Implement flexible checksums customization

### Miscellaneous
* Update to latest **smithy-kotlin** version
* Upgrade to Kotlin 1.8.10
* Refactor: track upstream module refactoring

## [0.20.0-beta] - 01/31/2023

### Features
* add ProcessCredentialsProvider which invokes a user-specified command to retrieve credentials
* Allow config override for one or more operations with an existing service client.

### Miscellaneous
* **Breaking** Remove `Closeable` supertype from `HttpClientEngine` interface. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/818) for more information.
* Refactor the way service client configuration is generated
* Upgrade Kotlin version to 1.8.0
* Update to latest AWS service models.
* Upgrade dependencies

## [0.19.5-beta] - 01/19/2023

### Miscellaneous
* Sync AWS service models.

## [0.19.4-beta] - 01/13/2023

### Miscellaneous
* Sync AWS models

## [0.19.3-beta] - 01/12/2023
**NOTE**: Do not use. Prefer 0.19.4-beta or later.

### Features
* [#122](https://github.com/awslabs/smithy-kotlin/issues/122) Add capability to intercept SDK operations

### Miscellaneous
* Sync AWS service models

## [0.19.2-beta] - 12/22/2022

### Fixes
* Correct validation of empty segments in ARN parser

### Miscellaneous
* Upgrade smithy-kotlin and sync AWS models.

## [0.19.1-beta] - 12/15/2022

### Miscellaneous
* Sync AWS service models

## [0.19.0-beta] - 12/01/2022

### Miscellaneous
* Upgrade smithy-kotlin and sync service models and partitions.
* **BREAKING** Refactor SDK I/O types. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/768) for more information

## [0.18.0-beta] - 11/23/2022

### Features
* Add support for dual-stack endpoints in client config.
* [#399](https://github.com/awslabs/aws-sdk-kotlin/issues/399) Add support for [S3 Virtual Host Addressing](https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html) (enabled by default).
* [#231](https://github.com/awslabs/aws-sdk-kotlin/issues/231) Add support for [S3 Access Points](https://aws.amazon.com/s3/features/access-points/).
* Add support for [S3 Object Lambda](https://aws.amazon.com/s3/features/object-lambda/).
* [#677](https://github.com/awslabs/smithy-kotlin/issues/677) Add a new tracing framework for centralized handling of log messages and metric events and providing easy integration points for connecting to downstream tracing systems (e.g., kotlin-logging)
* **BREAKING** Add smithy-modeled endpoint resolvers for AWS services. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/761) for more information.
* Add support for [S3 PrivateLink](https://docs.aws.amazon.com/AmazonS3/latest/userguide/privatelink-interface-endpoints.html).
* Add support for [FIPS](https://aws.amazon.com/compliance/fips/) endpoints in client config.
* Add support for [S3 Transfer Acceleration](https://docs.aws.amazon.com/AmazonS3/latest/userguide/transfer-acceleration.html).
* Add support for [S3 Outposts](https://aws.amazon.com/s3/outposts/).

### Miscellaneous
* Sync AWS service models

## [0.17.12-beta] - 11/15/2022

### Fixes
* [#753](https://github.com/awslabs/aws-sdk-kotlin/issues/753) Upgrade smithy-kotlin to fix Android crash when OkHttp response body coroutine throws an exception

### Miscellaneous
* Sync AWS models to latest

## [0.17.11-beta] - 11/10/2022

### Miscellaneous
* Sync AWS service models

## [0.17.10-beta] - 11/03/2022

### Miscellaneous
* Upgrade smithy to 1.26.1.
* Sync models and bump smithy-kotlin version for release.

## [0.17.9-beta] - 10/27/2022

### Fixes
* #711 Pass client configuration's httpClientEngine to the CredentialsProvider and region to ProfileCredentialsProvider
* [#733](https://github.com/awslabs/aws-sdk-kotlin/issues/733) Fix OkHttp engine crashing on Android when coroutine is cancelled while uploading request body

## [0.17.8-beta] - 10/14/2022

### Features
* #707 Support static stability for IMDS credentials

### Fixes
* [#715](https://github.com/awslabs/aws-sdk-kotlin/issues/715) Enable intra-repo links in API ref docs

### Miscellaneous
* Sync AWS service models

## [0.17.7-beta] - 10/03/2022

### Features
* #486 Enable configurability of the retry strategy through environment variables, system properties, and AWS profiles.

### Fixes
* [#697](https://github.com/awslabs/aws-sdk-kotlin/issues/697) Correct handling of non-success responses when retrieving credentials on ECS.

### Miscellaneous
* Upgrade smithy-kotlin version.
* Update/clarify changelog and add commit instructions in the Contributing Guidelines
* Upgrade Kotlin version and dependencies in ECS credentials integration test.
* Sync AWS models and upgrade smithy-kotlin.

## [0.17.6-beta] - 09/19/2022

### Features
* [#543](https://github.com/awslabs/aws-sdk-kotlin/issues/543) Add support for event streams
* Mark event stream HTTP body as duplex stream

### Fixes
* [#694](https://github.com/awslabs/aws-sdk-kotlin/issues/694) Merge per-op custom metadata to avoid clobbering per-client metadata

### Miscellaneous
* Sync AWS service models
* Update smithy-kotlin version
* Add unbound event stream payload deserialization path
* Use explict CoroutineScope for consuming event stream flow

## [0.17.5-beta] - 08/18/2022

### Fixes
* [#55](https://github.com/awslabs/aws-crt-kotlin/issues/55) Upgrade smithy-kotlin dependency to fix Mac dlopen issue
* [#601](https://github.com/awslabs/aws-sdk-kotlin/issues/601) Remove incorrect `.` at end of license identifier header in source files.

### Documentation
* [#683](https://github.com/awslabs/aws-sdk-kotlin/issues/683) Enhance **CONTRIBUTING.md** with additional details about required PR checks and how to run them locally

### Miscellaneous
* Upgrade smithy-kotlin to latest released version, 0.12.5
* Upgrade ktlint to 0.46.1.
* Sync AWS service models
* Upgrade Smithy to 1.23.0, upgrade Smithy Gradle to 0.6.0

## [0.17.4-beta] - 08/11/2022

### Fixes
* Update event stream model test template

### Miscellaneous
* Upgrade Kotlin version to 1.7.10
* Upgrade smithy-kotlin to 0.12.4.

## [0.17.3-beta] - 08/04/2022

### Miscellaneous
* Sync AWS service models

## [0.17.2-beta] - 07/28/2022

### Miscellaneous
* Sync AWS service models.
* [#216](https://github.com/awslabs/smithy-kotlin/issues/216) Enable [Explicit API mode](https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md)

## [0.17.1-beta] - 07/21/2022

### Features
* [#509](https://github.com/awslabs/aws-sdk-kotlin/issues/509) Implement ID prefix trimming for route53 resources.

### Miscellaneous
* Sync AWS service models.

## [0.17.0-beta] - 07/14/2022

### Fixes
* **Breaking**: Move DSL overloads on generated clients to extension methods

### Miscellaneous
* Sync AWS service models.
* **Breaking**: Upgrade **smithy-kotlin** version which will replace all instances of `Set<T>` with `List<T>` in service shapes

## [0.16.6-beta] - 07/08/2022

### Features
* [#123](https://github.com/awslabs/smithy-kotlin/issues/123) Add support for smithy Document type.

### Miscellaneous
* Update AWS models to latest versions
* Upgrade smithy-kotlin version to 0.11.2
* [#599](https://github.com/awslabs/smithy-kotlin/issues/599) Upgrade Smithy version to 1.22

## [0.16.5-beta] - 07/01/2022

### Miscellaneous
* [#622](https://github.com/awslabs/aws-sdk-kotlin/issues/622) Upgrade Kotlin to 1.7

## [0.16.4-beta] - 06/23/2022

### Fixes
* [#139](https://github.com/awslabs/smithy-kotlin/issues/139) Validate that members bound to URI paths are non-null at object construction

### Miscellaneous
* Upgrade smithy kotlin to [0.11.0](https://github.com/awslabs/smithy-kotlin/releases/tag/v0.11.0)

## [0.16.3-beta] - 06/16/2022

### Features
* Support bootstrapping services by package name (in addition to by model filename)

### Documentation
* Update the debugging guide to demonstrate how to use Log4j2 for logging

### Miscellaneous
* Sync AWS models to latest

## [0.16.2-beta] - 06/10/2022

### Fixes
* [#619](https://github.com/awslabs/aws-sdk-kotlin/issues/619), [#657](https://github.com/awslabs/smithy-kotlin/issues/657) Upgrade smithy-kotlin to pull in fixes for signing bugs.

### Documentation
* [#620](https://github.com/awslabs/aws-sdk-kotlin/issues/620) Update outdated howto docs to correctly describe client instantiation and client engine configuration

### Miscellaneous
* Sync AWS models to latest

## [0.16.1-beta] - 06/02/2022

### Features
* [#617](https://github.com/awslabs/smithy-kotlin/issues/617) Add a new non-CRT SigV4 signer and use it as the default. This removes the CRT as a hard dependency for using the SDK (although the CRT signer can still be used via explicit configuration on client creation).

### Miscellaneous
* Sync AWS models to latest

## [0.16.0] - 05/26/2022

### Features
* [#460](https://github.com/awslabs/aws-sdk-kotlin/issues/460) Enhance generic codegen to be more KMP-friendly. This is a **breaking change** which means service client artifacts will now include their platform name (e.g., `s3-jvm-<version>.jar` vs `s3-<version>.jar`). Users consuming dependencies through the Gradle Kotlin plugin will have this handled automatically for them.

### Fixes
* [#480](https://github.com/awslabs/aws-sdk-kotlin/issues/480) Upgrade smithy-kotlin to 0.10.0 which upgrades to ktor-2.x. This is considered a **breaking change** as it may reverse the issue described in #480 and break ktor-1.x users.

### Miscellaneous
* Upgrade smithy-kotlin to 0.9.2 which includes codegen updates to generate operations with all optional inputs to include a default parameter. See [smithy-kotlin#129](https://github.com/awslabs/smithy-kotlin/issues/129)
* upgrade kotlin to 1.6.21 and other deps to latest

## [0.15.2-beta] - 05/13/2022

### Features
* Implement recursion detection middleware.
* [#575](https://github.com/awslabs/aws-sdk-kotlin/issues/575) Add support for detecting custom metadata in system properties (starting with `aws.customMetadata.`) and environment variables (starting with `AWS_CUSTOM_METADATA_`)

## [0.15.1-beta] - 04/29/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Documentation
* update API reference doc styling

### Miscellaneous
* Sync latest AWS service models
* Refactor hashing functions into new subproject

## [0.15.0] - 04/29/2022

**NOTE**: Do not use. Prefer 0.15.1-beta or later.

## [0.14.4-beta] - 04/21/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* CreateMultipartUpload doesn't get signed correctly [#588](https://github.com/awslabs/aws-sdk-kotlin/issues/588)
* Possible memory leak in new default HTTP engine [#587](https://github.com/awslabs/aws-sdk-kotlin/issues/587)

### Miscellaneous
* sync AWS models [#590](https://github.com/awslabs/aws-sdk-kotlin/pull/590)

## [0.14.3-beta] - 04/14/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* resolve region only when profile credentials require it [#582](https://github.com/awslabs/aws-sdk-kotlin/pull/582)
* only set Content-Type when appropriate [#570](https://github.com/awslabs/aws-sdk-kotlin/pull/570)

### Miscellaneous
* sync AWS models [#585](https://github.com/awslabs/aws-sdk-kotlin/pull/585)

## [0.14.2-beta] - 04/07/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes

* fix timeout on large requests [#572](https://github.com/awslabs/aws-sdk-kotlin/issues/572)

## [0.14.1-beta] - 03/31/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* implement KMP XML serde and remove XmlPull dependency [#563](https://github.com/awslabs/aws-sdk-kotlin/pull/563)

### Miscellaneous
* sync AWS service models [#564](https://github.com/awslabs/aws-sdk-kotlin/pull/564)

## [0.14.0-beta] - 03/24/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* replace default HTTP client engine [#554](https://github.com/awslabs/aws-sdk-kotlin/pull/554)

### New features
* bootstrap event streams [#545](https://github.com/awslabs/aws-sdk-kotlin/pull/545)

### Fixes
* temporarily bypass httpchecksum traits until full flexible checksum support is available [#558](https://github.com/awslabs/aws-sdk-kotlin/pull/558)
* include headers in presigning requests [#556](https://github.com/awslabs/aws-sdk-kotlin/pull/556)
* backfill optional auth trait for cognito and cognito-idp [#555](https://github.com/awslabs/aws-sdk-kotlin/pull/555)

### Miscellaneous
* update AWS models to latest versions [#559](https://github.com/awslabs/aws-sdk-kotlin/pull/559)
* cleanup presign tests [#546](https://github.com/awslabs/aws-sdk-kotlin/pull/546)

## [0.13.1-beta] - 02/25/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* improve detection of available read bytes to avoid hang [#535](https://github.com/awslabs/aws-sdk-kotlin/pull/535)

## [0.13.0-beta] - 02/17/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* add sso credential provider; make all providers kmp compatible [#469](https://github.com/awslabs/aws-sdk-kotlin/pull/469)

### New features
* update AWS models to latest versions [#532](https://github.com/awslabs/aws-sdk-kotlin/pull/532)

### Fixes
* bump crt-kotlin to latest to fix leaked connections [#529](https://github.com/awslabs/aws-sdk-kotlin/pull/529)
* isClosedForRead implies availableForRead is zero
* fix CRT read channel buffer management

### Miscellaneous
* coroutine version bump to 1.6.0 and Duration stabilization [#514](https://github.com/awslabs/aws-sdk-kotlin/pull/514)
* dokka upgrade [#523](https://github.com/awslabs/aws-sdk-kotlin/pull/523)
* upgrade smithy to 1.17.0 [#521](https://github.com/awslabs/aws-sdk-kotlin/pull/521)

## [0.12.0-beta] - 02/04/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* enable waiters

### Fixes
* propagate crt stream errors to response body consumer [#510](https://github.com/awslabs/aws-sdk-kotlin/pull/510)

## [0.11.0-beta] - 01/20/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* Generate presigner into nested package for consistency [#502](https://github.com/awslabs/aws-sdk-kotlin/pull/502)

### New features
* update AWS models to latest versions [#505](https://github.com/awslabs/aws-sdk-kotlin/pull/505)

## [0.10.1-beta] - 01/13/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* update AWS models to latest versions [#499](https://github.com/awslabs/aws-sdk-kotlin/pull/499)
* Paginators! [smithy-kotlin#557](https://github.com/awslabs/smithy-kotlin/pull/557)

### Fixes
* enforce only once shutdown logic for crt engine connections [#497](https://github.com/awslabs/aws-sdk-kotlin/pull/497)

## [0.10.0-beta] - 01/06/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* Update codegen to generate base exception rather than UnknownServiceErrorException [#484](https://github.com/awslabs/aws-sdk-kotlin/pull/484)

### New features
* update AWS models to latest versions
* upgrade to Kotlin 1.6.10 [#474](https://github.com/awslabs/aws-sdk-kotlin/pull/474)

### Fixes
* Fix usage of unicode in bucket names of s3 presigner [#487](https://github.com/awslabs/aws-sdk-kotlin/pull/487)
* Add new services in published release [#468](https://github.com/awslabs/aws-sdk-kotlin/pull/468)

### Miscellaneous
* Add design tenets [#466](https://github.com/awslabs/aws-sdk-kotlin/pull/466)
* updated the Readme doc to include API reference guide [#471](https://github.com/awslabs/aws-sdk-kotlin/pull/471)

## [0.9.5-beta] - 12/09/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* amplifyuibuilder
* appconfigdata
* backupgateway
* chimesdkmeetings
* drs
* evidently
* inspector2
* iottwinmaker
* migrationhubrefactorspaces
* migrationhubstrategy
* rbin
* resiliencehub
* rum
* workspacesweb

### Fixes
* move endpoint resolution log messages from DEBUG to TRACE [#443](https://github.com/awslabs/aws-sdk-kotlin/pull/443)
* presigner cleanup [#452](https://github.com/awslabs/aws-sdk-kotlin/pull/452)
* import signing test suite [#451](https://github.com/awslabs/aws-sdk-kotlin/pull/451)

## [0.9.4-beta] - 12/01/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## [0.9.3-alpha] - 11/19/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* render default endpoint resolver for machinelearning [#424](https://github.com/awslabs/aws-sdk-kotlin/pull/424)

## [0.9.2-alpha] - 11/11/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* SDK generation and build docs [#377](https://github.com/awslabs/aws-sdk-kotlin/pull/377)

### Fixes
* disable signing for sts operations AssumeRoleWithSaml and AssumeRoleWithWebIdentity [#407](https://github.com/awslabs/aws-sdk-kotlin/pull/407)

### Miscellaneous
* Add howto to override default http client. [#412](https://github.com/awslabs/aws-sdk-kotlin/pull/412)

## [0.9.1-alpha] - 11/04/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* implement retries for imds [#404](https://github.com/awslabs/aws-sdk-kotlin/pull/404)
* enable machinelearning endpoint customization [#378](https://github.com/awslabs/aws-sdk-kotlin/pull/378)
* add glacier request body checksum [#379](https://github.com/awslabs/aws-sdk-kotlin/pull/379)

### Fixes
* restJson1 empty httpPayload body serialization [#402](https://github.com/awslabs/aws-sdk-kotlin/pull/402)

## [0.9.0-alpha] - 10/28/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes

* overhaul endpoint resolver types [#361](https://github.com/awslabs/aws-sdk-kotlin/pull/361)

### New features

* extend user agent metadata with framework, feature, and config [#372](https://github.com/awslabs/aws-sdk-kotlin/pull/372)

### Misc

* add sync models task and sync latest models [#374](https://github.com/awslabs/aws-sdk-kotlin/pull/374)

## [0.8.0-alpha] - 10/21/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* require a resolved configuration [#351](https://github.com/awslabs/aws-sdk-kotlin/pull/351)

### New features
* detect region from active AWS profile [#344](https://github.com/awslabs/aws-sdk-kotlin/pull/344)
* ec2 imds region provider [#341](https://github.com/awslabs/aws-sdk-kotlin/pull/341)
* Add STS assume role and web identity credential providers [#352](https://github.com/awslabs/aws-sdk-kotlin/pull/352)
* ECS credential provider [#353](https://github.com/awslabs/aws-sdk-kotlin/pull/353)
* ec2 credentials provider [#348](https://github.com/awslabs/aws-sdk-kotlin/pull/348)

### Fixes
* use wrapped response when deserializing modeled exceptions [#358](https://github.com/awslabs/aws-sdk-kotlin/pull/358)
* switch from ULong to Long in public presigner API for better java interop [#359](https://github.com/awslabs/aws-sdk-kotlin/pull/359)

### Misc
* Bump Kotlin and Dokka versions to latest release [#360](https://github.com/awslabs/aws-sdk-kotlin/pull/360)
* update aws models [#347](https://github.com/awslabs/aws-sdk-kotlin/pull/347)
* add docs for enabling logging in unit tests [#339](https://github.com/awslabs/aws-sdk-kotlin/pull/339)


## [0.7.0-alpha] - 10/14/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## New features

* http engine config [#336](https://github.com/awslabs/aws-sdk-kotlin/pull/336)
* add codegen wrappers for retries [#331](https://github.com/awslabs/aws-sdk-kotlin/pull/331)

## [0.6.0-alpha] - 10/07/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## New features

* implement basic retry support in runtime [#328](https://github.com/awslabs/aws-sdk-kotlin/pull/328)
* event stream framing support [#320](https://github.com/awslabs/aws-sdk-kotlin/pull/320)
* replace GSON based JSON serde with KMP compatible impl [#477](https://github.com/awslabs/smithy-kotlin/pull/477)
* Add IMDSv2 client in runtime [#330](https://github.com/awslabs/aws-sdk-kotlin/pull/330)


## [0.5.0-alpha] - 09/30/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes

* split auth and signing packages [#318](https://github.com/awslabs/aws-sdk-kotlin/issues/318)
    * Import paths changed to `aws.sdk.kotlin.runtime.auth.credentials` and `aws.sdk.kotlin.runtime.auth.signing`

### New features

* autofill Glacier accountId [#246](https://github.com/awslabs/aws-sdk-kotlin/issues/246)
* support JVM system property and environment variables for profiles [#297](https://github.com/awslabs/aws-sdk-kotlin/issues/297)
* expose method to sign standalone requests [#318](https://github.com/awslabs/aws-sdk-kotlin/issues/318)
* AWS configuration loader and parser [#216](https://github.com/awslabs/aws-sdk-kotlin/issues/216)

### Fixes

* utilize custom endpoint ports [#310](https://github.com/awslabs/aws-sdk-kotlin/issues/310)
* Replace junit imports with kotlin.test imports where possible [#321](https://github.com/awslabs/aws-sdk-kotlin/issues/321)
* update readme to include latest version [#319](https://github.com/awslabs/aws-sdk-kotlin/issues/319)
* sync models and endpoints [#317](https://github.com/awslabs/aws-sdk-kotlin/issues/317)
* Favor kotlin-test-juint5 over kotlin-test to resolve intermittent build failures [#316](https://github.com/awslabs/aws-sdk-kotlin/issues/316)
* kotlin 1.5.30, coroutine, kotest version bumps [#307](https://github.com/awslabs/aws-sdk-kotlin/issues/307)


## [0.4.0-alpha] - 08/26/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* ec2
* location
* marketplacecommerceanalytics

### New features

* Support for presigning requests [#435](https://github.com/awslabs/smithy-kotlin/issues/435)
* Detect aws region from system properties [#202](https://github.com/awslabs/aws-sdk-kotlin/issues/202)
* EC2 Query Protocol [#230](https://github.com/awslabs/aws-sdk-kotlin/issues/230)
* Provide opt-in wire logging [#425](https://github.com/awslabs/smithy-kotlin/issues/425)
* Support profile credentials provider [#302](https://github.com/awslabs/smithy-kotlin/issues/302)

### Fixes

* s3.deleteObjects causes an exception [#125](https://github.com/awslabs/aws-sdk-kotlin/issues/125)
* Streaming request BodyStream never read [#282](https://github.com/awslabs/aws-sdk-kotlin/issues/282)
* location service references traits not in sdk classpath [#286](https://github.com/awslabs/aws-sdk-kotlin/issues/286)
* Ignore unboxed types for subset of services [#261](https://github.com/awslabs/aws-sdk-kotlin/issues/261)
* Service operations specifying no auth should not sign requests with sigv4 [#263](https://github.com/awslabs/aws-sdk-kotlin/issues/263)
* Create S3 object with Unicode name fails with signature mismatch [#200](https://github.com/awslabs/aws-sdk-kotlin/issues/200)
* Codegen errors in marketplacecommerceanalytics [#214](https://github.com/awslabs/aws-sdk-kotlin/issues/214)
* Escape model-extra files for Windows [#191](https://github.com/awslabs/aws-sdk-kotlin/issues/191)
* Support Glacier APIVersion Header [#165](https://github.com/awslabs/smithy-kotlin/issues/165)
* Support APIGateway Accept Header [#157](https://github.com/awslabs/smithy-kotlin/issues/157)
* Add support for awsQueryError trait [#375](https://github.com/awslabs/smithy-kotlin/issues/375)
* S3 HeadObject errors require customization [#152](https://github.com/awslabs/aws-sdk-kotlin/issues/152)
* S3 custom treatment of GetBucketLocation response [#194](https://github.com/awslabs/aws-sdk-kotlin/issues/194)

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
