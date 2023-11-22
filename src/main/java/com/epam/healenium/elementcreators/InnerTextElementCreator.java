package com.epam.healenium.elementcreators;

import com.epam.healenium.treecomparing.Node;

public class InnerTextElementCreator implements ElementCreator{
    @Override
    public String create(Node node) {
        return node.getInnerText();
    }
}
