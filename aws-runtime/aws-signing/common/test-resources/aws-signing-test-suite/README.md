Where did the files in this directory come from?
================================================

These test files were taken from the [aws-c-auth test suite](https://github.com/awslabs/aws-c-auth/tree/main/tests/aws-signing-test-suite).
The original test suite comes from the (now defunct) Signature Version 4 Test Suite documentation
from the [AWS General Reference](https://docs.aws.amazon.com/general/latest/gr/Welcome.html).

Signature Version 4 Test Suite
------------------------------

To assist you in the development of an AWS client that supports Signature Version 4, you can use the
files in the test suite to ensure your code is performing each step of the signing process correctly.

Each test group contains files that you can use to validate each of the tasks described in
Signature Version 4 Signing Process. The following list describes the contents of each file.

- request.txt - the request to be signed.
- context.json - signing configuration
- header-canonical-request.txt - the resulting canonical request
- header-string-to-sign.txt - the resulting string to sign.
- header-signature.txt - the signature
- header-signed-request.txt - the signed request

There may also be `query-*` versions of each which have the same meaning but are used when signing via query instead
of headers.

The examples in the test suite use the following credential scope by default:

```
AKIDEXAMPLE/20150830/us-east-1/service/aws4_request
```

The example secret key used for signing is:

```
wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY
```

