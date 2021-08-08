package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class HealeniumSelectorImitatorDto {

    private Node targetNode;
    private Locator userSelector;

}
