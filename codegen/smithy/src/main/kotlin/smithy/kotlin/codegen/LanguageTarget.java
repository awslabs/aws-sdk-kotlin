/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package smithy.kotlin.codegen;

/**
 * Represents a possible language target that can be generated.
 */
public enum LanguageTarget {
    /**
     * Node-specific language target.
     */
    JVM {
        @Override
        String getTemplateFileName() {
            return "runtimeConfig.ts.template";
        }
    },

    /**
     * Browser-specific language target.
     */
    JS {
        @Override
        String getTemplateFileName() {
            return "runtimeConfig.browser.ts.template";
        }
    },

    /**
     * A language target that shares configuration that is shared across all
     * runtimes.
     */
    SHARED {
        @Override
        String getTemplateFileName() {
            return "runtimeConfig.shared.ts.template";
        }
    };

    abstract String getTemplateFileName();

    String getTargetFilename() {
        return getTemplateFileName().replace(".template", "");
    }
}
