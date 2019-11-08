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
package com.epam.healenium.elementcreators;

import com.epam.healenium.utils.ResourceReader;
import com.epam.healenium.treecomparing.Node;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

public class AttributesElementCreator implements ElementCreator {

    private static final Set<String> SKIPPED_ATTRIBUTES =
        ResourceReader.readResource("skippedAttributes.txt", s -> s.collect(Collectors.toSet()));

    @Override
    public String create(Node node) {
        return node.getOtherAttributes().entrySet()
            .stream()
            .filter(entry -> StringUtils.isNoneBlank(entry.getKey(), entry.getValue()))
            .filter(entry -> !SKIPPED_ATTRIBUTES.contains(entry.getKey()))
            .map(entry -> String.format("[%s=\"%s\"]", entry.getKey().trim(), entry.getValue().trim()))
            .collect(Collectors.joining());
    }
}
