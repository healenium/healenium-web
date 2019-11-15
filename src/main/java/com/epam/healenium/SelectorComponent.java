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
package com.epam.healenium;

import com.epam.healenium.elementcreators.ClassElementCreator;
import com.epam.healenium.elementcreators.PathElementCreator;
import com.epam.healenium.elementcreators.PositionElementCreator;
import com.epam.healenium.elementcreators.AttributesElementCreator;
import com.epam.healenium.elementcreators.ElementCreator;
import com.epam.healenium.elementcreators.IdElementCreator;
import com.epam.healenium.elementcreators.ParentElementCreator;
import com.epam.healenium.elementcreators.TagElementCreator;
import com.epam.healenium.treecomparing.Node;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SelectorComponent {
    PATH(new PathElementCreator()),
    PARENT(new ParentElementCreator()),
    TAG(new TagElementCreator()),
    ID(new IdElementCreator()),
    CLASS(new ClassElementCreator()),
    POSITION(new PositionElementCreator()),
    ATTRIBUTES(new AttributesElementCreator());

    private final ElementCreator elementCreator;

    public String createComponent(Node node) {
        return elementCreator.create(node);
    }
}
