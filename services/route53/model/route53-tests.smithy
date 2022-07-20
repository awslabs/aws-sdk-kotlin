$version: "1.0"

namespace com.amazonaws.route53

use smithy.test#httpRequestTests


apply ListResourceRecordSets @httpRequestTests([
    {
        id: "ListResourceRecordSetsNoTrim",
        documentation: "Validates that HostedZoneId isn't trimmed when not prefixed.",
        method: "GET",
        protocol: "aws.protocols#restXml",
        uri: "/2013-04-01/hostedzone/IDOFMYHOSTEDZONE/rrset",
        bodyMediaType: "application/xml",
        params: {
            "HostedZoneId": "IDOFMYHOSTEDZONE"
        }
    },
    {
        id: "ListResourceRecordSetsTrim",
        documentation: "Validates that HostedZoneId is trimmed.",
        method: "GET",
        protocol: "aws.protocols#restXml",
        uri: "/2013-04-01/hostedzone/IDOFMYHOSTEDZONE/rrset",
        bodyMediaType: "application/xml",
        params: {
            "HostedZoneId": "hostedzone/IDOFMYHOSTEDZONE"
        }
    },
    {
        id: "ListResourceRecordSetsTrimLeadingSlash",
        documentation: "Validates that HostedZoneId is trimmed even with a leading slash.",
        method: "GET",
        protocol: "aws.protocols#restXml",
        uri: "/2013-04-01/hostedzone/IDOFMYHOSTEDZONE/rrset",
        bodyMediaType: "application/xml",
        params: {
            "HostedZoneId": "/hostedzone/IDOFMYHOSTEDZONE"
        }
    },
    {
        id: "ListResourceRecordSetsTrimMultislash",
        documentation: "Validates that HostedZoneId isn't over-trimmed.",
        method: "GET",
        protocol: "aws.protocols#restXml",
        uri: "/2013-04-01/hostedzone/IDOFMY%2FHOSTEDZONE/rrset",
        bodyMediaType: "application/xml",
        params: {
            "HostedZoneId": "/hostedzone/IDOFMY/HOSTEDZONE"
        }
    },
])

apply GetChange @httpRequestTests([
    {
        id: "GetChangeTrimChangeId",
        documentation: "This test validates that change id is correctly trimmed",
        method: "GET",
        protocol: "aws.protocols#restXml",
        uri: "/2013-04-01/change/SOMECHANGEID",
        bodyMediaType: "application/xml",
        params: {
            "Id": "/change/SOMECHANGEID"
        }
    },
])

apply GetReusableDelegationSet @httpRequestTests([
    {
        id: "GetReusableDelegationSetTrimDelegationSetId",
        documentation: "This test validates that delegation set id is correctly trimmed",
        method: "GET",
        protocol: "aws.protocols#restXml",
        uri: "/2013-04-01/delegationset/DELEGATIONSETID",
        bodyMediaType: "application/xml",
        params: {
            "Id": "/delegationset/DELEGATIONSETID"
        }
    },
])