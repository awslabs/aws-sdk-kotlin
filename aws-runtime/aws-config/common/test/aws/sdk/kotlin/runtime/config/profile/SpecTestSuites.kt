/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.profile

/**
 * This test suite exercises the parser and continuation merger.
 */
// language=JSON
internal const val PARSER_TEST_SUITE_JSON = """
{
  "description": [
    "These are test descriptions that describe how to convert a raw configuration and credentials file into an ",
    "in-memory representation of the profile file.",
    "See 'parser-tests.schema.json' for a description of this file's structure."
  ],
  "tests": [
    {
      "name": "Empty files have no profiles.",
      "input": {
        "configFile": ""
      },
      "output": {}
    },
    {
      "name": "Empty profiles have no properties.",
      "input": {
        "configFile": "[profile foo]"
      },
      "output": {
        "profiles": {
          "foo": {}
        }
      }
    },
    {
      "name": "Profile definitions must end with brackets.",
      "input": {
        "configFile": "[profile foo"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Profile names should be trimmed.",
      "input": {
        "configFile": "[profile \tfoo \t]"
      },
      "output": {
        "profiles": {
          "foo": {}
        }
      }
    },
    {
      "name": "Tabs can separate profile names from profile prefix.",
      "input": {
        "configFile": "[profile\tfoo]"
      },
      "output": {
        "profiles": {
          "foo": {}
        }
      }
    },
    {
      "name": "Properties must be defined in a profile.",
      "input": {
        "configFile": "name = value"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Profiles can contain properties.",
      "input": {
        "configFile": "[profile foo]\nname = value"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "Windows style line endings are supported.",
      "input": {
        "configFile": "[profile foo]\r\nname = value"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "Equals signs are supported in property values.",
      "input": {
        "configFile": "[profile foo]\nname = val=ue"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "val=ue"
          }
        }
      }
    },
    {
      "name": "Unicode characters are supported in property values.",
      "input": {
        "configFile": "[profile foo]\nname = ðŸ˜‚"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "ðŸ˜‚"
          }
        }
      }
    },
    {
      "name": "Profiles can contain multiple properties.",
      "input": {
        "configFile": "[profile foo]\nname = value\nname2 = value2"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value",
            "name2": "value2"
          }
        }
      }
    },
    {
      "name": "Property keys and values are trimmed.",
      "input": {
        "configFile": "[profile foo]\nname \t=  \tvalue \t"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "Property values can be empty.",
      "input": {
        "configFile": "[profile foo]\nname ="
      },
      "output": {
        "profiles": {
          "foo": {
            "name": ""
          }
        }
      }
    },
    {
      "name": "Property key cannot be empty.",
      "input": {
        "configFile": "[profile foo]\n= value"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Property definitions must contain an equals sign.",
      "input": {
        "configFile": "[profile foo]\nkey : value"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Multiple profiles can be empty.",
      "input": {
        "configFile": "[profile foo]\n[profile bar]"
      },
      "output": {
        "profiles": {
          "foo": {},
          "bar": {}
        }
      }
    },
    {
      "name": "Multiple profiles can have properties.",
      "input": {
        "configFile": "[profile foo]\nname = value\n[profile bar]\nname2 = value2"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          },
          "bar": {
            "name2": "value2"
          }
        }
      }
    },
    {
      "name": "Blank lines are ignored.",
      "input": {
        "configFile": "\t \n[profile foo]\n\t\n \nname = value\n\t \n[profile bar]\n \t"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          },
          "bar": {}
        }
      }
    },
    {
      "name": "Pound sign comments are ignored.",
      "input": {
        "configFile": "# Comment\n[profile foo] # Comment\nname = value # Comment with # sign"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "Semicolon sign comments are ignored.",
      "input": {
        "configFile": "; Comment\n[profile foo] ; Comment\nname = value ; Comment with ; sign"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "All comment types can be used together.",
      "input": {
        "configFile": "# Comment\n[profile foo] ; Comment\nname = value # Comment with ; sign"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "Comments can be empty.",
      "input": {
        "configFile": ";\n[profile foo];\nname = value ;\n"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "Comments can be adjacent to profile names.",
      "input": {
        "configFile": "[profile foo]; Adjacent semicolons\n[profile bar]# Adjacent pound signs"
      },
      "output": {
        "profiles": {
          "foo": {},
          "bar": {}
        }
      }
    },
    {
      "name": "Comments adjacent to values are included in the value.",
      "input": {
        "configFile": "[profile foo]\nname = value; Adjacent semicolons\nname2 = value# Adjacent pound signs"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value; Adjacent semicolons",
            "name2": "value# Adjacent pound signs"
          }
        }
      }
    },
    {
      "name": "Property values can be continued on the next line.",
      "input": {
        "configFile": "[profile foo]\nname = value\n -continued"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value\n-continued"
          }
        }
      }
    },
    {
      "name": "Property values can be continued with multiple lines.",
      "input": {
        "configFile": "[profile foo]\nname = value\n -continued\n -and-continued"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value\n-continued\n-and-continued"
          }
        }
      }
    },
    {
      "name": "Continuations are trimmed.",
      "input": {
        "configFile": "[profile foo]\nname = value\n \t -continued \t "
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value\n-continued"
          }
        }
      }
    },
    {
      "name": "Continuation values include pound comments.",
      "input": {
        "configFile": "[profile foo]\nname = value\n -continued # Comment"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value\n-continued # Comment"
          }
        }
      }
    },
    {
      "name": "Continuation values include semicolon comments.",
      "input": {
        "configFile": "[profile foo]\nname = value\n -continued ; Comment"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value\n-continued ; Comment"
          }
        }
      }
    },
    {
      "name": "Continuations cannot be used outside of a profile.",
      "input": {
        "configFile": " -continued"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Continuations cannot be used outside of a property.",
      "input": {
        "configFile": "[profile foo]\n -continued"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Continuations reset with profile definitions.",
      "input": {
        "configFile": "[profile foo]\nname = value\n[profile foo]\n -continued"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Duplicate profiles in the same file merge properties.",
      "input": {
        "configFile": "[profile foo]\nname = value\n[profile foo]\nname2 = value2"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value",
            "name2": "value2"
          }
        }
      }
    },
    {
      "name": "Duplicate properties in a profile use the last one defined.",
      "input": {
        "configFile": "[profile foo]\nname = value\nname = value2"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value2"
          }
        }
      }
    },
    {
      "name": "Duplicate properties in duplicate profiles use the last one defined.",
      "input": {
        "configFile": "[profile foo]\nname = value\n[profile foo]\nname = value2"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value2"
          }
        }
      }
    },
    {
      "name": "Default profile with profile prefix overrides default profile without prefix when profile prefix is first.",
      "input": {
        "configFile": "[profile default]\nname = value\n[default]\nname2 = value2"
      },
      "output": {
        "profiles": {
          "default": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "Default profile with profile prefix overrides default profile without prefix when profile prefix is last.",
      "input": {
        "configFile": "[default]\nname2 = value2\n[profile default]\nname = value"
      },
      "output": {
        "profiles": {
          "default": {
            "name": "value"
          }
        }
      }
    },
    {
      "name": "Invalid profile names are ignored.",
      "input": {
        "configFile": "[profile in valid]\nname = value",
        "credentialsFile": "[in valid 2]\nname2 = value2"
      },
      "output": {}
    },
    {
      "name": "Invalid property names are ignored.",
      "input": {
        "configFile": "[profile foo]\nin valid = value"
      },
      "output": {
        "profiles": {
          "foo": {}
        }
      }
    },
    {
      "name": "All valid profile name characters are supported.",
      "input": {
        "configFile": "[profile ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_]"
      },
      "output": {
        "profiles": {
          "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_": {}
        }
      }
    },
    {
      "name": "All valid property name characters are supported.",
      "input": {
        "configFile": "[profile foo]\nABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_ = value"
      },
      "output": {
        "profiles": {
          "foo": {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_": "value"
          }
        }
      }
    },
    {
      "name": "Properties can have sub-properties.",
      "input": {
        "configFile": "[profile foo]\ns3 =\n name = value"
      },
      "output": {
        "profiles": {
          "foo": {
            "s3": {
              "name": "value"
            }
          }
        }
      }
    },
    {
      "name": "Invalid sub-property definitions cause an error.",
      "input": {
        "configFile": "[profile foo]\ns3 =\n invalid"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Sub-property definitions can have an empty value.",
      "input": {
        "configFile": "[profile foo]\ns3 =\n name ="
      },
      "output": {
        "profiles": {
          "foo": {
            "s3": {
              "name": ""
            }
          }
        }
      }
    },
    {
      "name": "Sub-property definitions cannot have an empty name.",
      "input": {
        "configFile": "[profile foo]\ns3 =\n = value"
      },
      "output": {
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "Sub-property definitions ignore invalid name.",
      "input": {
        "configFile": "[profile foo]\ns3 =\n in valid = value\n valid = value"
      },
      "output": {
        "profiles": {
          "foo": {
            "s3": {
              "valid": "value"
            }
          }
        }
      }
    },
    {
      "name": "Sub-properties can have blank lines that are ignored",
      "input": {
        "configFile": "[profile foo]\ns3 =\n name = value\n\t \n name2 = value2"
      },
      "output": {
        "profiles": {
          "foo": {
            "s3": {
              "name": "value",
              "name2": "value2"
            }
          }
        }
      }
    },
    {
      "name": "Profiles duplicated in multiple files are merged.",
      "input": {
        "configFile": "[profile foo]\nname = value",
        "credentialsFile": "[foo]\nname2 = value2"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value",
            "name2": "value2"
          }
        }
      }
    },
    {
      "name": "Default profiles with mixed prefixes in the config file ignore the one without prefix when merging.",
      "input": {
        "configFile": "[profile default]\nname = value\n[default]\nname2 = value2\n[profile default]\nname3 = value3"
      },
      "output": {
        "profiles": {
          "default": {
            "name": "value",
            "name3": "value3"
          }
        }
      }
    },
    {
      "name": "Default profiles with mixed prefixes merge with credentials",
      "input": {
        "configFile": "[profile default]\nname = value\n[default]\nname2 = value2\n[profile default]\nname3 = value3",
        "credentialsFile": "[default]\nsecret=foo"
      },
      "output": {
        "profiles": {
          "default": {
            "name": "value",
            "name3": "value3",
            "secret": "foo"
          }
        }
      }
    },
    {
      "name": "Duplicate properties between files uses credentials property.",
      "input": {
        "configFile": "[profile foo]\nname = value",
        "credentialsFile": "[foo]\nname = value2"
      },
      "output": {
        "profiles": {
          "foo": {
            "name": "value2"
          }
        }
      }
    },
    {
      "name": "Config profiles without prefix are ignored.",
      "input": {
        "configFile": "[foo]\nname = value"
      },
      "output": {}
    },
    {
      "name": "Credentials profiles with prefix are ignored.",
      "input": {
        "credentialsFile": "[profile foo]\nname = value"
      },
      "output": {}
    },

    {
      "name": "Comment characters adjacent to profile decls",
      "input": {
        "configFile": "[profile foo]; semicolon\n[profile bar]# pound"
      },
      "output": {
        "profiles": {
          "foo": {},
          "bar": {}
        }
      }
    },
    {
      "name": "sneaky profile name",
      "input": {
        "configFile": "[profilefoo]\nname = value\n[profile bar]"
      },
      "output": {
        "profiles": { "bar":  {} }
      }
    },
    {
      "name": "properties from an invalid profile name are ignored",
      "input": {
        "configFile": "[profile foo]\nname = value\n[profile in valid]\nx = 1\n[profile bar]\nname = value2"
      },
      "output": {
        "profiles": { "bar":  {"name":  "value2"}, "foo": {"name":  "value"} }
      }
    },
    {
      "name": "profile name with extra whitespace",
      "input": {
        "configFile": "[   profile foo    ]\nname = value\n[profile bar]"
      },
      "output": {
        "profiles": { "bar":  {}, "foo": {"name":  "value"} }
      }
    },
    {
      "name": "profile name with extra whitespace in credentials",
      "input": {
        "credentialsFile": "[   foo    ]\nname = value\n[profile bar]"
      },
      "output": {
        "profiles": { "foo": {"name":  "value"} }
      }
    },
    {
      "name": "handles different sequences of continuations and sub-properties",
      "input": {
        "configFile": "[default]\nname = value1\n value2 = 12 \n value3\nname2 = value2\nname3 =\n name3_0 = 1"
      },
      "output": {
        "profiles": {
          "default": {
            "name": "value1\nvalue2 = 12\nvalue3",
            "name2": "value2",
            "name3": {
              "name3_0": "1"
            }
          }
        }
      }
    },
    {
      "name": "SSO Session in credentials file is invalid",
      "input": {
        "credentialsFile": "[sso-session my-session]\nname = value1"
      },
      "output": { 
        "errorContaining": "Encountered unexpected token"
      }
    },
    {
      "name": "SSO Session in config file is parsed",
      "input": {
        "configFile": "[sso-session my-session]\nname = value1"
      },
      "output": {
        "sso-sessions": {
            "my-session": {
                "name": "value1"
            }
        }
      }
    },
    {
      "name": "SSO Session in config file with same name as profile",
      "input": {
        "configFile": "[sso-session foo]\nname = value1\n[profile foo]\nname2 = value2\n"
      },
      "output": {
        "profiles": {
            "foo": {
                "name2": "value2"
            }
        },
        "sso-sessions": {
            "foo": {
                "name": "value1"
            }
        }
      }
    },
    {
      "name": "SSO session name with extra whitespace",
      "input": {
        "configFile": "[   sso-session foo    ]\nname = value\n[profile bar]"
      },
      "output": {
        "profiles": { "bar":  {} },
        "sso-sessions": { "foo": {"name":  "value"} }
      }
    },
    {
      "name": "Default SSO session is ignored",
      "input": {
        "configFile": "[sso-session]\nname = value\n"
      },
      "output": {}
    },
    {
      "name": "Services section is successfully parsed",
      "input": {
        "configFile": "[default]\nservices = my-services\n[services my-services]\ns3 =\n\tendpoint_url = https://s3-endpoint-override.aws"
      },
      "output": {
        "profiles": {
          "default": { 
            "services": "my-services"
          }
        },
        "services": {
          "my-services": {
            "s3": {
              "endpoint_url": "https://s3-endpoint-override.aws"
            }
          }
        }
      }
    },
    {
      "name": "Unknown section is successfully parsed",
      "input": {
        "configFile": "[default]\nregion = us-weast-4\n[foobar my-custom-section]\napple =\n\tbanana = cherry"
      },
      "output": {
        "profiles": {
          "default": { 
            "region": "us-weast-4"
          }
        },
        "unknown": {
          "my-custom-section": {
            "apple": {
              "banana": "cherry"
            }
          }
        }
      }
    },
    {
      "name": "SSO Session in config file with same name as default profile",
      "input": {
        "configFile": "[default]\nname = profile-default\n[sso-session default]\nname = sso-session-default"
      },
      "output": {
        "profiles": {
          "default": {
            "name": "profile-default"
          }
        },
        "sso-sessions": {
            "default": {
                "name": "sso-session-default"
            }
        }
      }
    }
  ]
}
"""

// language=JSON
internal const val LOADER_TEST_SUITE_JSON = """
{
  "description": [
    "These are test descriptions that specify which files and profiles should be loaded based on the specified environment ",
    "variables.",
    "See 'file-location-tests.schema.json' for a description of this file's structure."
  ],

  "tests": [
    {
      "name": "User home is loaded from HOME with highest priority on non-windows platforms.",
      "environment": {
        "HOME": "/home/user",
        "USERPROFILE": "ignored",
        "HOMEDRIVE": "ignored",
        "HOMEPATH": "ignored"
      },
      "languageSpecificHome": "ignored",
      "platform": "linux",
      "profile": "default",
      "configLocation": "/home/user/.aws/config",
      "credentialsLocation": "/home/user/.aws/credentials"
    },

    {
      "name": "User home is loaded using language-specific resolution on non-windows platforms when HOME is not set.",
      "environment": {
        "USERPROFILE": "ignored",
        "HOMEDRIVE": "ignored",
        "HOMEPATH": "ignored"
      },
      "languageSpecificHome": "/home/user",
      "platform": "linux",
      "profile": "default",
      "configLocation": "/home/user/.aws/config",
      "credentialsLocation": "/home/user/.aws/credentials"
    },

    {
      "name": "User home is loaded from HOME with highest priority on windows platforms.",
      "environment": {
        "HOME": "C:\\users\\user",
        "USERPROFILE": "ignored",
        "HOMEDRIVE": "ignored",
        "HOMEPATH": "ignored"
      },
      "languageSpecificHome": "ignored",
      "platform": "windows",
      "profile": "default",
      "configLocation": "C:\\users\\user\\.aws\\config",
      "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
    },

    {
      "name": "User home is loaded from USERPROFILE on windows platforms when HOME is not set.",
      "environment": {
        "USERPROFILE": "C:\\users\\user",
        "HOMEDRIVE": "ignored",
        "HOMEPATH": "ignored"
      },
      "languageSpecificHome": "ignored",
      "platform": "windows",
      "profile": "default",
      "configLocation": "C:\\users\\user\\.aws\\config",
      "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
    },

    {
      "name": "User home is loaded from HOMEDRIVEHOMEPATH on windows platforms when HOME and USERPROFILE are not set.",
      "environment": {
        "HOMEDRIVE": "C:",
        "HOMEPATH": "\\users\\user"
      },
      "languageSpecificHome": "ignored",
      "platform": "windows",
      "profile": "default",
      "configLocation": "C:\\users\\user\\.aws\\config",
      "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
    },

    {
      "name": "User home is loaded using language-specific resolution on windows platforms when no environment variables are set.",
      "environment": {
      },
      "languageSpecificHome": "C:\\users\\user",
      "platform": "windows",
      "profile": "default",
      "configLocation": "C:\\users\\user\\.aws\\config",
      "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
    },

    {
      "name": "The default config location can be overridden by the user on non-windows platforms.",
      "environment": {
        "AWS_CONFIG_FILE": "/other/path/config",
        "HOME": "/home/user"
      },
      "platform": "linux",
      "configLocation": "/other/path/config",
      "credentialsLocation": "/home/user/.aws/credentials"
    },

    {
      "name": "The default credentials location can be overridden by the user on non-windows platforms.",
      "environment": {
        "AWS_SHARED_CREDENTIALS_FILE": "/other/path/credentials",
        "HOME": "/home/user"
      },
      "platform": "linux",
      "profile": "default",
      "configLocation": "/home/user/.aws/config",
      "credentialsLocation": "/other/path/credentials"
    },

    {
      "name": "The default credentials location can be overridden by the user on windows platforms.",
      "environment": {
        "AWS_CONFIG_FILE": "C:\\other\\path\\config",
        "HOME": "C:\\users\\user"
      },
      "platform": "windows",
      "profile": "default",
      "configLocation": "C:\\other\\path\\config",
      "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
    },

    {
      "name": "The default credentials location can be overridden by the user on windows platforms.",
      "environment": {
        "AWS_SHARED_CREDENTIALS_FILE": "C:\\other\\path\\credentials",
        "HOME": "C:\\users\\user"
      },
      "platform": "windows",
      "profile": "default",
      "configLocation": "C:\\users\\user\\.aws\\config",
      "credentialsLocation": "C:\\other\\path\\credentials"
    },

    {
      "name": "The default profile can be overridden via environment variable.",
      "environment": {
        "AWS_PROFILE": "other",
        "HOME": "/home/user"
      },
      "platform": "linux",
      "profile": "other",
      "configLocation": "/home/user/.aws/config",
      "credentialsLocation": "/home/user/.aws/credentials"
    }
  ]
}        
"""
