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
package com.epam.healenium;

import com.epam.healenium.annotation.DisableHealing;
import com.epam.healenium.client.RestClient;
import com.epam.healenium.model.Locator;
import com.epam.healenium.service.HealingService;
import com.epam.healenium.service.NodeService;
import com.epam.healenium.treecomparing.JsoupHTMLParser;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.StackUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Data
public class SelfHealingEngine {

    private static final Config DEFAULT_CONFIG = ConfigFactory.systemProperties().withFallback(
            ConfigFactory.load("healenium.properties").withFallback(ConfigFactory.load()));

    private final Config config;
    private final WebDriver webDriver;
    private final double scoreCap;

    private RestClient client;
    private NodeService nodeService;
    private HealingService healingService;

    /**
     * @param delegate a delegate driver, not actually {@link SelfHealingDriver} instance.
     * @param config   user-defined configuration
     */
    public SelfHealingEngine(@NotNull WebDriver delegate, @NotNull Config config) {
        // merge given config with default values
        Config finalizedConfig = ConfigFactory.load(config).withFallback(DEFAULT_CONFIG);

        this.webDriver = delegate;
        this.config = finalizedConfig;
        this.scoreCap = finalizedConfig.getDouble("score-cap");

        this.client = new RestClient(finalizedConfig);
        this.nodeService = new NodeService(delegate);
        this.healingService = new HealingService(finalizedConfig, delegate);
    }

    /**
     * Used, when client not override config explicitly
     *
     * @param delegate webdriver
     */
    public SelfHealingEngine(@NotNull WebDriver delegate) {
        this(delegate, DEFAULT_CONFIG);
    }

    /**
     * Stores the valid locator state: the element it found and the page.
     *
     * @param by          the locator
     * @param webElements the elements while it is still accessible by the locator
     */
    public void saveElements(PageAwareBy by, List<WebElement> webElements) {
        List<List<Node>> nodesToSave = webElements.stream()
                .map(nodeService::getNodePath)
                .collect(Collectors.toList());
        saveNodes(by, nodesToSave);
    }

    public void saveNodes(PageAwareBy key, List<List<Node>> elementsToSave) {
        client.selectorsRequest(key.getBy(), new ArrayList<>(elementsToSave), getCurrentUrl());
    }

    public Optional<Scored<By>> toLocator(List<Locator> imitatedLocators, Double score) {
        for (Locator imitatedLocator : imitatedLocators) {
            By locator = StackUtils.BY_MAP.get(imitatedLocator.getType()).apply(imitatedLocator.getValue());
            List<WebElement> elements = webDriver.findElements(locator);
            if (elements.size() == 1) {
                return Optional.of(new Scored<>(score, locator));
            }
        }
        return Optional.empty();
    }

    public String pageSource() {
        if (webDriver instanceof JavascriptExecutor) {
            return ((JavascriptExecutor) webDriver).executeScript("return document.body.outerHTML;").toString();
        } else {
            return webDriver.getPageSource();
        }
    }

    public Node parseTree(String tree) {
        return new JsoupHTMLParser().parse(new ByteArrayInputStream(tree.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean isHealingEnabled() {
        boolean isDisabled = StackUtils.isAnnotationPresent(DisableHealing.class);
        return config.getBoolean("heal-enabled") && !isDisabled;
    }

    public boolean isHealingBacklighted() {
        return config.getBoolean("backlight-healing");
    }

    public String getCurrentUrl() {
        return webDriver.getCurrentUrl().split("://")[1];
    }

    public byte[] captureScreen(WebElement element) {
        if (isHealingBacklighted()) {
            JavascriptExecutor jse = (JavascriptExecutor) webDriver;
            jse.executeScript("arguments[0].style.border='3px solid red'", element);
        }
        return ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
    }
}
