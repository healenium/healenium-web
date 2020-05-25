package com.epam.healenium.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealingResultDto {

    private Locator locator;
    private Double score;

}
