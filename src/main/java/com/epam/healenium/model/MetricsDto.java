package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Node;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MetricsDto {

    private Node targetNode;
    private HealingCandidateDto mainHealingCandidate;
    private List<HealingCandidateDto> otherHealingCandidates;
    private Locator userSelector;
    private Locator healedSelector;
    private String currentDom;
    private String previousSuccessfulDom;
}
