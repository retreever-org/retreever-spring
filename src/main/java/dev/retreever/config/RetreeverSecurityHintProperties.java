/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "retreever.security")
public class RetreeverSecurityHintProperties {

    private boolean hintLog = true;

    public boolean isHintLog() {
        return hintLog;
    }

    public void setHintLog(boolean hintLog) {
        this.hintLog = hintLog;
    }
}
