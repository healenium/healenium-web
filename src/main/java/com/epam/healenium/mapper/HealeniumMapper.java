/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.mapper;

import com.epam.healenium.model.Context;
import com.epam.healenium.model.HealingResult;
import com.epam.healenium.model.HealingResultDto;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.HealingResultRequestDto;
import com.epam.healenium.model.RequestDto;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.StackUtils;
import org.openqa.selenium.By;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class HealeniumMapper {

    public RequestDto buildDto(By by, String currentUrl) {
        StackTraceElement traceElement = StackUtils.findOriginCaller(Thread.currentThread().getStackTrace())
                .orElseThrow(() -> new IllegalArgumentException("Failed to detect origin method caller"));
        String[] locatorParts = by.toString().split(":", 2);
        RequestDto dto = new RequestDto()
                .setLocator(locatorParts[1].trim())
                .setType(locatorParts[0].trim());
        dto.setClassName(traceElement.getClassName());
        dto.setMethodName(traceElement.getMethodName());
        dto.setUrl(currentUrl);
        return dto;
    }

    public RequestDto buildDto(By by, List<List<Node>> nodePath, String currentUrl) {
        RequestDto dto = buildDto(by, currentUrl);
        dto.setNodePath(nodePath);
        return dto;
    }

    public RequestDto buildDto(By by, String page, List<Scored<By>> healingResults,
                               Scored<By> selected, byte[] screenshot, String currentUrl) {
        RequestDto dto = buildDto(by, currentUrl);
        dto.setPageContent(page)
                .setResults(buildResultDto(healingResults))
                .setUsedResult(buildResultDto(selected))
                .setScreenshot(screenshot);
        return dto;
    }

    public HealingResultDto buildResultDto(Scored<By> scored) {
        return new HealingResultDto(byToLocator(scored.getValue()), scored.getScore());
    }

    public List<HealingResultDto> buildResultDto(Collection<Scored<By>> scored) {
        return scored.stream().map(this::buildResultDto).collect(Collectors.toList());
    }

    public Locator byToLocator(By by) {
        String[] locatorParts = by.toString().split(":", 2);
        return new Locator(locatorParts[1].trim(), locatorParts[0].trim());
    }

    public List<Locator> byToLocator(Collection<By> by) {
        return by.stream().map(this::byToLocator).collect(Collectors.toList());
    }

    public HealingResultRequestDto buildMultRequest(Context context, HealingResult healingResult,
                                                    List<Scored<By>> choices, String metrics) {
        RequestDto requestDto = buildDto(context.getPageAwareBy().getBy(),
                context.getPageContent(), choices, choices.get(0),
                healingResult.getScreenshot(), context.getCurrentUrl());
        HealingResultRequestDto resultRequest = new HealingResultRequestDto();
        resultRequest
                .setRequestDto(requestDto)
                .setMetrics(metrics)
                .setHealingTime(healingResult.getHealingTime());
        return resultRequest;
    }
}
