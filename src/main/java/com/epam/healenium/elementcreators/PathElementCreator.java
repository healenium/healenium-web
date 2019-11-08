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

import java.util.ArrayDeque;
import java.util.Deque;

public class PathElementCreator implements ElementCreator {

    private PositionElementCreator positionCreator = new PositionElementCreator();

    @Override
    public String create(Node node) {
        Node current = node;
        Deque<String> path = new ArrayDeque<>();
        while (current != null) {
            String item = current.getTag();
            if (hasSimilarNeighbours(current)) {
                item += positionCreator.create(current);
            }
            path.addFirst(item);
            current = current.getParent();
        }
        return String.join(" > ", path);
    }

    private boolean hasSimilarNeighbours(Node current) {
        Node parent = current.getParent();
        if (parent == null) {
            return false;
        }
        return parent.getChildren()
            .stream()
            .map(Node::getTag)
            .filter(tag -> tag.equals(current.getTag()))
            .count() > 1L;
    }
}
