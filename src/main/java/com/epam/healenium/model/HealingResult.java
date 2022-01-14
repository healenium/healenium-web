package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class HealingResult {

    private List<Node> paths;
    private List<Scored<Node>> targetNodes;
    private List<HealingCandidateDto> allHealingCandidates;
    private List<HealedElement> healedElements = new ArrayList<>();
    private MetricsDto metricsDto;
    private String healingTime;
    private byte[] screenshot;
}
