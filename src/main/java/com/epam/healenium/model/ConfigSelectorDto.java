package com.epam.healenium.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ConfigSelectorDto {

    private boolean urlForKey;
    private boolean pathForKey;
    private List<SelectorDto> disableHealingElementDto;
    private List<SelectorDto> enableHealingElementsDto;

}
