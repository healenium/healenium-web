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
package com.epam.healenium.service.impl;

import com.epam.healenium.PageAwareBy;
import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.model.MetricsDto;
import com.epam.healenium.service.HealingService;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.StackUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.Augmenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class HealingServiceImpl implements HealingService {

    private final SelfHealingEngine engine;
    private final WebDriver driver;

    public HealingServiceImpl(SelfHealingEngine engine) {
        this.engine = engine;
        this.driver = engine.getWebDriver();
    }

    public Optional<WebElement> heal(PageAwareBy pageBy, NoSuchElementException ex) {
        // check if already have healed results
//        RequestDto request = mapper.buildDto(pageBy.getBy(), e.getStackTrace(), pageSource());
        return healLocators(pageBy, null, ex.getStackTrace()).map(driver::findElement);
    }

    public List<WebElement> healElements(PageAwareBy pageBy, StackTraceElement[] stackTrace, NoSuchElementException ex) {
        List<List<Node>> nodesToHeal = engine.findNodesToHeal(pageBy, stackTrace);
        List<WebElement> resultWebElements = getHealedElementsByNodes(pageBy, stackTrace, nodesToHeal);
        return Optional.of(resultWebElements).orElseThrow(() -> ex);
    }

    public List<WebElement> saveAndHealElements(PageAwareBy pageBy, List<WebElement> pageElements,
                                                StackTraceElement[] stackTrace) {
        List<List<Node>> nodesToHeal = engine.findNodesToHeal(pageBy, stackTrace);
        engine.savePath(pageBy, pageElements, Optional.of(nodesToHeal).orElse(Collections.emptyList()));
        return Stream.concat(pageElements.stream(), getHealedElementsByNodes(pageBy, stackTrace, nodesToHeal).stream())
                .collect(Collectors.toList());
    }

    @NotNull
    private List<WebElement> getHealedElementsByNodes(PageAwareBy pageBy, StackTraceElement[] stackTrace, List<List<Node>> nodesToHeal) {
        List<WebElement> resultWebElements = new ArrayList<>();
        nodesToHeal.forEach(nodes ->
                healLocators(pageBy, nodes, stackTrace)
                        .map(driver::findElement)
                        .ifPresent(resultWebElements::add));
        return resultWebElements;
    }

    /**
     * @param pageBy PageAwareBy class
     * @param trace  list of StackTraceElements
     * @param nodes  list of nodes
     * @return By locator
     */
    public Optional<By> healLocators(PageAwareBy pageBy, List<Node> nodes, StackTraceElement[] trace) {
        // collect page content
        String pageContent = pageSource();
        // search target point in stacktrace
        Optional<StackTraceElement> traceElement = StackUtils.findOriginCaller(trace);
        // search for possible healing results
        MetricsDto metricsDto = new MetricsDto()
                .setCurrentDom(pageContent)
                .setUserSelector(engine.getClient().getMapper().byToLocator(pageBy.getBy()));
        Optional<List<List<Node>>> paths = traceElement
                .map(it -> engine.getClient().getLastHealingData(pageBy.getBy(), it))
                .filter(Optional::isPresent)
                .map(dto -> {
                    metricsDto.setPreviousSuccessfulDom(dto.get().getPageContent());
                    return dto.get().getPaths();
                });

        List<Scored<By>> choices = nodes == null
                ? engine.findNewLocations(pageSource(), paths, metricsDto)
                : engine.findNewLocationsByNodes(nodes, pageSource(), metricsDto);
        String healingTime = engine.getHealingTime();
        Optional<Scored<By>> result = choices.stream().findFirst();
        if (!result.isPresent()) {
            log.warn("New element locators have not been found");
            double scoreCap = engine.getScoreCap();
            log.warn("Score property={} is bigger than healing's locator score", scoreCap);
        } else {
            Scored<By> healed = result.get();
            log.warn("Using healed locator: {}", result);
            byte[] screenshot = captureScreen(healed);
            metricsDto.setHealedSelector(engine.getClient().getMapper().byToLocator(healed.getValue()));
            traceElement.ifPresent(it -> {
                // build request and send it to server
                engine.getClient().healRequest(pageBy.getBy(), it, pageContent, choices, healed, screenshot, healingTime, metricsDto);
            });
        }
        return result.map(Scored::getValue);
    }

    /**
     * Create screenshot of healed element
     *
     * @param byScored - healed locator
     * @return path to screenshot location
     */
    //TODO: need pass search context here
    private byte[] captureScreen(Scored<By> byScored) {
        WebElement element = driver.findElement(byScored.getValue());
        if (engine.isHealingBacklighted()) {
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("arguments[0].style.border='3px solid red'", element);
        }
        WebDriver augmentedDriver = new Augmenter().augment(driver);
        return ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BYTES);
    }

    private String pageSource() {
        if (driver instanceof JavascriptExecutor) {
            return ((JavascriptExecutor) driver).executeScript("return document.body.outerHTML;").toString();
        } else {
            return driver.getPageSource();
        }
    }

}
