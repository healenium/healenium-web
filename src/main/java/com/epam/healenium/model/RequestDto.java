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
package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Node;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Accessors(chain = true)
@Data
public class RequestDto {

    // currently used selector
    private String locator;
    private String type;
    private String className;
    private String methodName;
    // page where search was performed
    private String pageContent;
    // searched element path
    private List<Node> nodePath = Collections.emptyList();
    // healed selectors
    private List<HealingResultDto> results;
    // used selector for healing
    private HealingResultDto usedResult;
    private byte[] screenshot;
}
