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
import com.epam.healenium.client.callback.HttpCallback;
import com.epam.healenium.function.EmptyUrlFunction;
import com.epam.healenium.function.FullUrlFunction;
import com.epam.healenium.message.MessageAction;
import com.epam.healenium.model.ConfigSelectorDto;
import com.epam.healenium.model.Context;
import com.epam.healenium.model.HealedElement;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.RequestDto;
import com.epam.healenium.model.SelectorDto;
import com.epam.healenium.model.SessionContext;
import com.epam.healenium.service.HealingService;
import com.epam.healenium.service.NodeService;
import com.epam.healenium.treecomparing.JsoupHTMLParser;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.StackUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


@Slf4j(topic = "healenium")
@Data
public class SelfHealingEngine {

    private static final Config DEFAULT_CONFIG = ConfigFactory.systemProperties().withFallback(
            ConfigFactory.load("healenium.properties").withFallback(ConfigFactory.load()));

    private final Config config;
    private final WebDriver webDriver;
    private final double scoreCap;
    private final boolean isProxy;

    private RestClient client;
    private NodeService nodeService;
    private HealingService healingService;
    private SessionContext sessionContext;

    /**
     * @param delegate a delegate driver, not actually {@link SelfHealingDriver} instance.
     * @param config   user-defined configuration
     */
    public SelfHealingEngine(@NotNull WebDriver delegate, @NotNull Config config) {
        // merge given config with default values
        Config finalizedConfig = ConfigFactory.load(config).withFallback(DEFAULT_CONFIG)
                .withValue("sessionKey", ConfigValueFactory.fromAnyRef(((RemoteWebDriver) delegate).getSessionId().toString()));

        this.webDriver = delegate;
        this.config = finalizedConfig;
        this.scoreCap = finalizedConfig.getDouble("score-cap");
        this.isProxy = finalizedConfig.getBoolean("proxy");
    }

    /**
     * Used, when client not override config explicitly
     *
     * @param delegate webdriver
     */
    public SelfHealingEngine(@NotNull WebDriver delegate) {
        this(delegate, DEFAULT_CONFIG);
    }

    public void saveElements(Context context, List<WebElement> webElements) {
        try {
            String by = context.getBy().toString();
            List<String> ids = context.getElementIds();
            Map<String, List<String>> sessionSelectors = sessionContext.getSessionSelectors();
            List<String> storedIds = sessionSelectors.get(by);
            if (storedIds == null || (!storedIds.containsAll(ids) && storedIds.size() != ids.size())) {
                sessionSelectors.put(by, ids);
                RequestDto requestDto = client.getMapper().buildDto(context.getBy(), context.getAction(), null);
                requestDto.setElementIds(ids);
                requestDto.setSessionId(((RemoteWebDriver) webDriver).getSessionId().toString());
                requestDto.setNodePath(getNodePath(webElements));
                String currentUrl = getCurrentUrl();
                context.setCurrentUrl(currentUrl);
                requestDto.setUrl(currentUrl);
                client.saveElements(requestDto);
            }
        } catch (Exception e) {
            log.warn("[Save Elements] Error during save elements: {}. Message: {}. Exception: {}",
                    context.getElementIds(), e.getMessage(), e);
        }
    }

    public List<List<Node>> getNodePath(List<WebElement> webElements) {
        return webElements.stream()
                .map(e -> nodeService.getNodePath(webDriver, e))
                .collect(Collectors.toList());
    }

    public void replaceHealedElementLocator(List<Locator> imitatedLocators, Double score, HealedElement healedElement) {
        for (Locator imitatedLocator : imitatedLocators) {
            By locator = StackUtils.BY_MAP.get(imitatedLocator.getType()).apply(imitatedLocator.getValue());
            List<WebElement> elements = webDriver.findElements(locator);
            if (elements.size() == 1 && ((RemoteWebElement) elements.get(0)).getId().equals(((RemoteWebElement) healedElement.getElement()).getId())) {
                healedElement.setScored(new Scored<>(score, locator));
            }
        }
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

    public BiFunction<SelfHealingEngine, String, String> getUrlFunction(boolean urlForKey) {
        return urlForKey ? new FullUrlFunction() : new EmptyUrlFunction();
    }

    public byte[] captureScreen(WebElement element) {
        if (isHealingBacklighted()) {
            JavascriptExecutor jse = (JavascriptExecutor) webDriver;
            jse.executeScript("arguments[0].style.border='3px solid red'", element);
        }
        return ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
    }

    public String getCurrentUrl() {
        return webDriver.getCurrentUrl();
    }

    public void loadStoredSelectors() {
        ConfigSelectorDto configSelectorDto = client.getElements();
        if (configSelectorDto != null) {
            List<SelectorDto> disableHealingElementDto = configSelectorDto.getDisableHealingElementDto();
            List<SelectorDto> enableHealingElementsDto = configSelectorDto.getEnableHealingElementsDto();
            BiFunction<SelfHealingEngine, String, String> urlFunction = getUrlFunction(configSelectorDto.isUrlForKey());
            sessionContext = new SessionContext()
                    .setFunctionUrl(urlFunction)
                    .setEnableHealingElements(enableHealingElementsDto.stream()
                            .collect(Collectors.toMap(SelectorDto::getId, SelectorDto::getLocator)))
                    .setDisableHealingElement(disableHealingElementDto.stream()
                            .collect(Collectors.toMap(SelectorDto::getId, SelectorDto::getLocator)));
        }
    }

    public void initReport() {
        client.initReport(((RemoteWebDriver) webDriver).getSessionId().toString());
    }

    public void quit() {
        HttpCallback httpCallback = client.getHttpCallback();
        httpCallback.updateActiveMessageAmount(MessageAction.PUSH);
        try {
            httpCallback.getCountDownLatch().await();
            webDriver.quit();
        } catch (InterruptedException e) {
            log.warn("Error during quit call. Message: {}. Exception: {}", e.getMessage(), e);
        }
    }

}
