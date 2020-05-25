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
package com.epam.healenium.converter;

import com.epam.healenium.FieldName;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.NodeBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Map;

@SuppressWarnings("unchecked")
public class NodeDeserializer extends JsonDeserializer<Node> {

    @Override
    public Node deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = parser.getCodec();
        TreeNode tree = parser.readValueAsTree();
        String tag = codec.treeToValue(tree.path(FieldName.TAG), String.class);
        Integer index = codec.treeToValue(tree.path(FieldName.INDEX), Integer.class);
        String innerText = codec.treeToValue(tree.path(FieldName.INNER_TEXT), String.class);
        String id = codec.treeToValue(tree.path(FieldName.ID), String.class);
        String classes = codec.treeToValue(tree.path(FieldName.CLASSES), String.class);
        Map<String, String> attributes = codec.treeToValue(tree.path(FieldName.OTHER), Map.class);
        attributes.put(FieldName.ID, id);
        attributes.put(FieldName.CLASS, classes);
        return new NodeBuilder()
                .setTag(tag)
                .setIndex(index)
                .addContent(innerText)
                .setAttributes(attributes)
                .build();
    }

}
