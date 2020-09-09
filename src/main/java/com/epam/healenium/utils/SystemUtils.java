/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.UnknownHostException;

@Slf4j
@UtilityClass
public class SystemUtils {
    
    public String getHostProjectName() {
        String projectPath = System.getProperty("user.dir");
        return projectPath.substring(projectPath.lastIndexOf("/") + 1);
    }

    public String getHostIpAddress() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            log.warn("Failed to get host address", ex);
        }
        return StringUtils.EMPTY;
    }
}
