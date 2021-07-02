package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class HealingCandidateDto {

    private Double score;
    private Integer LCSDistance;
    private Integer curPathHeight;
    private Node node;

}
