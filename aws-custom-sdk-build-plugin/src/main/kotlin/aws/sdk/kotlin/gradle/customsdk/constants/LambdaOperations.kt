/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk.constants

/**
 * Type-safe operation constants for Lambda service.
 * 
 * These constants can be used in the AWS Custom SDK Build plugin DSL
 * to specify which operations to include in custom client builds.
 * 
 * Generated during SDK build process.
 */
object LambdaOperations {
/**
 * Operation: AddLayerVersionPermission
 */
const val AddLayerVersionPermission = "AddLayerVersionPermission"
/**
 * Operation: AddPermission
 */
const val AddPermission = "AddPermission"
/**
 * Operation: CreateAlias
 */
const val CreateAlias = "CreateAlias"
/**
 * Operation: CreateCodeSigningConfig
 */
const val CreateCodeSigningConfig = "CreateCodeSigningConfig"
/**
 * Operation: CreateEventSourceMapping
 */
const val CreateEventSourceMapping = "CreateEventSourceMapping"
/**
 * Operation: CreateFunction
 */
const val CreateFunction = "CreateFunction"
/**
 * Operation: CreateFunctionUrlConfig
 */
const val CreateFunctionUrlConfig = "CreateFunctionUrlConfig"
/**
 * Operation: DeleteAlias
 */
const val DeleteAlias = "DeleteAlias"
/**
 * Operation: DeleteCodeSigningConfig
 */
const val DeleteCodeSigningConfig = "DeleteCodeSigningConfig"
/**
 * Operation: DeleteEventSourceMapping
 */
const val DeleteEventSourceMapping = "DeleteEventSourceMapping"
/**
 * Operation: DeleteFunction
 */
const val DeleteFunction = "DeleteFunction"
/**
 * Operation: DeleteFunctionCodeSigningConfig
 */
const val DeleteFunctionCodeSigningConfig = "DeleteFunctionCodeSigningConfig"
/**
 * Operation: DeleteFunctionConcurrency
 */
const val DeleteFunctionConcurrency = "DeleteFunctionConcurrency"
/**
 * Operation: DeleteFunctionEventInvokeConfig
 */
const val DeleteFunctionEventInvokeConfig = "DeleteFunctionEventInvokeConfig"
/**
 * Operation: DeleteFunctionUrlConfig
 */
const val DeleteFunctionUrlConfig = "DeleteFunctionUrlConfig"
/**
 * Operation: DeleteLayerVersion
 */
const val DeleteLayerVersion = "DeleteLayerVersion"
/**
 * Operation: DeleteProvisionedConcurrencyConfig
 */
const val DeleteProvisionedConcurrencyConfig = "DeleteProvisionedConcurrencyConfig"
/**
 * Operation: GetAccountSettings
 */
const val GetAccountSettings = "GetAccountSettings"
/**
 * Operation: GetAlias
 */
const val GetAlias = "GetAlias"
/**
 * Operation: GetCodeSigningConfig
 */
const val GetCodeSigningConfig = "GetCodeSigningConfig"
/**
 * Operation: GetEventSourceMapping
 */
const val GetEventSourceMapping = "GetEventSourceMapping"
/**
 * Operation: GetFunction
 */
const val GetFunction = "GetFunction"
/**
 * Operation: GetFunctionCodeSigningConfig
 */
const val GetFunctionCodeSigningConfig = "GetFunctionCodeSigningConfig"
/**
 * Operation: GetFunctionConcurrency
 */
const val GetFunctionConcurrency = "GetFunctionConcurrency"
/**
 * Operation: GetFunctionConfiguration
 */
const val GetFunctionConfiguration = "GetFunctionConfiguration"
/**
 * Operation: GetFunctionEventInvokeConfig
 */
const val GetFunctionEventInvokeConfig = "GetFunctionEventInvokeConfig"
/**
 * Operation: GetFunctionRecursionConfig
 */
const val GetFunctionRecursionConfig = "GetFunctionRecursionConfig"
/**
 * Operation: GetFunctionUrlConfig
 */
const val GetFunctionUrlConfig = "GetFunctionUrlConfig"
/**
 * Operation: GetLayerVersion
 */
const val GetLayerVersion = "GetLayerVersion"
/**
 * Operation: GetLayerVersionByArn
 */
const val GetLayerVersionByArn = "GetLayerVersionByArn"
/**
 * Operation: GetLayerVersionPolicy
 */
const val GetLayerVersionPolicy = "GetLayerVersionPolicy"
/**
 * Operation: GetPolicy
 */
const val GetPolicy = "GetPolicy"
/**
 * Operation: GetProvisionedConcurrencyConfig
 */
const val GetProvisionedConcurrencyConfig = "GetProvisionedConcurrencyConfig"
/**
 * Operation: GetRuntimeManagementConfig
 */
const val GetRuntimeManagementConfig = "GetRuntimeManagementConfig"
/**
 * Operation: Invoke
 */
const val Invoke = "Invoke"
/**
 * Operation: InvokeAsync
 */
const val InvokeAsync = "InvokeAsync"
/**
 * Operation: InvokeWithResponseStream
 */
const val InvokeWithResponseStream = "InvokeWithResponseStream"
/**
 * Operation: ListAliases
 */
const val ListAliases = "ListAliases"
/**
 * Operation: ListCodeSigningConfigs
 */
const val ListCodeSigningConfigs = "ListCodeSigningConfigs"
/**
 * Operation: ListEventSourceMappings
 */
const val ListEventSourceMappings = "ListEventSourceMappings"
/**
 * Operation: ListFunctionEventInvokeConfigs
 */
const val ListFunctionEventInvokeConfigs = "ListFunctionEventInvokeConfigs"
/**
 * Operation: ListFunctionUrlConfigs
 */
const val ListFunctionUrlConfigs = "ListFunctionUrlConfigs"
/**
 * Operation: ListFunctions
 */
const val ListFunctions = "ListFunctions"
/**
 * Operation: ListFunctionsByCodeSigningConfig
 */
const val ListFunctionsByCodeSigningConfig = "ListFunctionsByCodeSigningConfig"
/**
 * Operation: ListLayerVersions
 */
const val ListLayerVersions = "ListLayerVersions"
/**
 * Operation: ListLayers
 */
const val ListLayers = "ListLayers"
/**
 * Operation: ListProvisionedConcurrencyConfigs
 */
const val ListProvisionedConcurrencyConfigs = "ListProvisionedConcurrencyConfigs"
/**
 * Operation: ListTags
 */
const val ListTags = "ListTags"
/**
 * Operation: ListVersionsByFunction
 */
const val ListVersionsByFunction = "ListVersionsByFunction"
/**
 * Operation: PublishLayerVersion
 */
const val PublishLayerVersion = "PublishLayerVersion"
/**
 * Operation: PublishVersion
 */
const val PublishVersion = "PublishVersion"
/**
 * Operation: PutFunctionCodeSigningConfig
 */
const val PutFunctionCodeSigningConfig = "PutFunctionCodeSigningConfig"
/**
 * Operation: PutFunctionConcurrency
 */
const val PutFunctionConcurrency = "PutFunctionConcurrency"
/**
 * Operation: PutFunctionEventInvokeConfig
 */
const val PutFunctionEventInvokeConfig = "PutFunctionEventInvokeConfig"
/**
 * Operation: PutFunctionRecursionConfig
 */
const val PutFunctionRecursionConfig = "PutFunctionRecursionConfig"
/**
 * Operation: PutProvisionedConcurrencyConfig
 */
const val PutProvisionedConcurrencyConfig = "PutProvisionedConcurrencyConfig"
/**
 * Operation: PutRuntimeManagementConfig
 */
const val PutRuntimeManagementConfig = "PutRuntimeManagementConfig"
/**
 * Operation: RemoveLayerVersionPermission
 */
const val RemoveLayerVersionPermission = "RemoveLayerVersionPermission"
/**
 * Operation: RemovePermission
 */
const val RemovePermission = "RemovePermission"
/**
 * Operation: TagResource
 */
const val TagResource = "TagResource"
/**
 * Operation: UntagResource
 */
const val UntagResource = "UntagResource"
/**
 * Operation: UpdateAlias
 */
const val UpdateAlias = "UpdateAlias"
/**
 * Operation: UpdateCodeSigningConfig
 */
const val UpdateCodeSigningConfig = "UpdateCodeSigningConfig"
/**
 * Operation: UpdateEventSourceMapping
 */
const val UpdateEventSourceMapping = "UpdateEventSourceMapping"
/**
 * Operation: UpdateFunctionCode
 */
const val UpdateFunctionCode = "UpdateFunctionCode"
/**
 * Operation: UpdateFunctionConfiguration
 */
const val UpdateFunctionConfiguration = "UpdateFunctionConfiguration"
/**
 * Operation: UpdateFunctionEventInvokeConfig
 */
const val UpdateFunctionEventInvokeConfig = "UpdateFunctionEventInvokeConfig"
/**
 * Operation: UpdateFunctionUrlConfig
 */
const val UpdateFunctionUrlConfig = "UpdateFunctionUrlConfig"
}