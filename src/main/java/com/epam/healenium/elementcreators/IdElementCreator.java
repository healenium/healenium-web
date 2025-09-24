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

import com.epam.healenium.treecomparing.Node;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class IdElementCreator implements ElementCreator {

    private static final Pattern SAFE_HASH_ID = Pattern.compile("^-?[A-Za-z_][A-Za-z0-9_-]*$");

    @Override
    public String create(Node node) {
        String id = StringUtils.trimToNull(node.getId());
        if (id == null) {
            return "";
        }
        if (isHashSafe(id)) {
            return "#" + id;
        }
        return "[id=\"" + escapeAttrValue(id) + "\"]";
    }

    private static boolean isHashSafe(String id) {
        return SAFE_HASH_ID.matcher(id).matches();
    }

    private static String escapeAttrValue(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
