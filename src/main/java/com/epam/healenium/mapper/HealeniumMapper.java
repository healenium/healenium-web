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

import com.epam.healenium.mapper.by.ByAllOrByChainedMapper;
import com.epam.healenium.mapper.by.ByDefaultMapper;
import com.epam.healenium.mapper.by.ByIdOrNameMapper;
import com.epam.healenium.mapper.by.ByRelativeMapper;
import com.epam.healenium.model.Context;
import com.epam.healenium.model.HealingResult;
import com.epam.healenium.model.HealingResultDto;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.RequestDto;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.StackTraceReader;
import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ByIdOrName;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.openqa.selenium.support.pagefactory.ByAll;
import org.openqa.selenium.support.pagefactory.ByChained;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HealeniumMapper {

    Map<Class<?>, Function<By, String[]>> BY_MAPPERS = ImmutableMap.<Class<?>, Function<By, String[]>>builder()
            .put(ByIdOrName.class, new ByIdOrNameMapper())
            .put(ByAll.class, new ByAllOrByChainedMapper())
            .put(ByChained.class, new ByAllOrByChainedMapper())
            .put(RelativeLocator.RelativeBy.class, new ByRelativeMapper())
            .build();

    private StackTraceReader stackTraceReader;

    public HealeniumMapper(StackTraceReader stackTraceReader) {
        this.stackTraceReader = stackTraceReader;
    }

    public RequestDto buildDto(By by, String command, String currentUrl) {
        StackTraceElement traceElement = stackTraceReader.findOriginCaller(Thread.currentThread().getStackTrace())
                .orElseThrow(() -> new IllegalArgumentException("Failed to detect origin method caller"));
        String[] locatorParts = getLocatorParts(by);
        RequestDto dto = new RequestDto()
                .setLocator(locatorParts[1].trim())
                .setType(locatorParts[0].trim())
                .setClassName(traceElement.getClassName())
                .setMethodName(traceElement.getMethodName())
                .setCommand(command)
                .setUrl(currentUrl);
        return dto;
    }

    public RequestDto buildDto(By by, String command, String page, List<Scored<By>> healingResults,
                               Scored<By> selected, byte[] screenshot, String currentUrl) {
        RequestDto dto = buildDto(by, command, currentUrl);
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
        String[] locatorParts = getLocatorParts(by);
        return new Locator(locatorParts[1].trim(), locatorParts[0].trim());
    }

    public List<Locator> byToLocator(Collection<By> by) {
        return by.stream().map(this::byToLocator).collect(Collectors.toList());
    }

    public RequestDto buildMultRequest(Context context, HealingResult healingResult,
                                                    List<Scored<By>> choices, String metrics) {
        return buildDto(context.getBy(), context.getAction(),
                context.getPageContent(), choices, choices.get(0),
                healingResult.getScreenshot(), context.getCurrentUrl())
                .setElementIds(context.getElementIds())
                .setMetrics(metrics);
    }

    public String[] getLocatorParts(By by) {
        return BY_MAPPERS.getOrDefault(by.getClass(), new ByDefaultMapper()).apply(by);
    }

}
