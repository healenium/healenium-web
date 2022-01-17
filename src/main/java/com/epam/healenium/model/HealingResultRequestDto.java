package com.epam.healenium.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class HealingResultRequestDto {

    private RequestDto requestDto;
    private String metrics;
    private String healingTime;

}
