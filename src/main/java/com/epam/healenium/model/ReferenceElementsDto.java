package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Node;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ReferenceElementsDto {

    private String pageContent;
    @ToString.Exclude
    private List<List<Node>> paths;
    private List<Locator> unsuccessfulLocators;
}
