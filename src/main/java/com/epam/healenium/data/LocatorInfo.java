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
package com.epam.healenium.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
public class LocatorInfo {

    private String reportName;
    private String endTime;
    private List<Entry> elementsInfo;

    public LocatorInfo() {
        this.reportName = "Healing Report";
        this.elementsInfo = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode
    public static class Entry {
        private String failedLocatorValue;
        private String failedLocatorType;
        private String healedLocatorValue;
        private String healedLocatorType;
        private String screenShotPath;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SimplePageEntry extends Entry {
        private String pageName;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PageAsClassEntry extends Entry {
        private String declaringClass;
        private String methodName;
        private Integer lineNumber;
        private String fileName;
    }
}
