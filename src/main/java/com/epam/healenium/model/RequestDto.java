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
