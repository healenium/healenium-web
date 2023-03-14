package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Node;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ReferenceElementsDto {

    private String pageContent;
    private List<List<Node>> paths;
}
