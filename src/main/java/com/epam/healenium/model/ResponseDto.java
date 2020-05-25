package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Node;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ResponseDto {

    private List<Node> nodePath = Collections.emptyList();

}
