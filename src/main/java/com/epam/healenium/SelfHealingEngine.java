/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium;

import static com.epam.healenium.SelectorComponent.ATTRIBUTES;
import static com.epam.healenium.SelectorComponent.CLASS;
import static com.epam.healenium.SelectorComponent.ID;
import static com.epam.healenium.SelectorComponent.PARENT;
import static com.epam.healenium.SelectorComponent.PATH;
import static com.epam.healenium.SelectorComponent.POSITION;
import static com.epam.healenium.SelectorComponent.TAG;

import com.epam.healenium.utils.ResourceReader;
import com.epam.healenium.data.FileSystemPathStorage;
import com.epam.healenium.data.LocatorInfo;
import com.epam.healenium.data.PathStorage;
import com.epam.healenium.treecomparing.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import com.typesafe.config.Config;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class SelfHealingEngine {

    /**
     * A JavaScript source to extract an HTML item with its attributes
     */
    private static final String SCRIPT =
            ResourceReader.readResource("itemsWithAttributes.js", s -> s.collect(Collectors.joining()));

    @Getter private final Config config;
    @Getter private final WebDriver webDriver;
    private final PathStorage storage;
    private final int recoveryTries;
    private final List<Set<SelectorComponent>> selectorDetailLevels;

    /**
     * @param delegate a delegate driver, not actually {@link SelfHealingDriver} instance.
     * @param config   user-defined configuration
     */
    SelfHealingEngine(WebDriver delegate, Config config) {
        this.webDriver = delegate;
        this.config = config;
        this.storage = new FileSystemPathStorage(config);
        this.recoveryTries = config.getInt("recovery-tries");

        List<Set<SelectorComponent>> temp = new ArrayList<>();
        temp.add(EnumSet.of(TAG, ID));
        temp.add(EnumSet.of(TAG, CLASS));
        temp.add(EnumSet.of(PARENT, TAG, ID, CLASS));
        temp.add(EnumSet.of(PARENT, TAG, CLASS, POSITION));
        temp.add(EnumSet.of(PARENT, TAG, ID, CLASS, ATTRIBUTES));
        temp.add(EnumSet.of(PATH));
        this.selectorDetailLevels = Collections.unmodifiableList(temp);
    }

    /**
     * Stores the valid locator state: the element it found and the page.
     *
     * @param by         the locator
     * @param webElement the element while it is still accessible by the locator
     */
    void savePath(PageAwareBy by, WebElement webElement) {
        log.info("* savePath start: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        List<Node> nodePath = getNodePath(webElement);
        storage.persistLastValidPath(by, by.getPageName(), nodePath);
        log.info("* savePath finish: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
    }

    @SneakyThrows
    void saveLocator(LocatorInfo info) {
        storage.saveLocatorInfo(info);
    }

    private List<Node> getNodePath(WebElement webElement) {
        log.info("* getNodePath start: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        JavascriptExecutor executor = (JavascriptExecutor) webDriver;
        String data = (String) executor.executeScript(SCRIPT, webElement);
        List<Node> path = new LinkedList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode treeNode = mapper.readTree(data);
            if (treeNode.isArray()) {
                for (final JsonNode jsonNode : treeNode) {
                    Node node = toNode(mapper.treeAsTokens(jsonNode));
                    path.add(node);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to get element node path!", ex);
        }
        log.info("* getNodePath finish: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        return path;
    }

    /**
     * Convert raw data to {@code Node}
     * @param parser - JSON reader
     * @return path node
     * @throws IOException
     */
    private Node toNode(JsonParser parser) throws IOException {
        ObjectCodec codec = parser.getCodec();
        TreeNode tree = parser.readValueAsTree();
        String tag = codec.treeToValue(tree.path("tag"), String.class);
        Integer index = codec.treeToValue(tree.path("index"), Integer.class);
        String innerText = codec.treeToValue(tree.path("innerText"), String.class);
        String id = codec.treeToValue(tree.path("id"), String.class);
        //noinspection unchecked
        Set<String> classes = codec.treeToValue(tree.path("classes"), Set.class);
        //noinspection unchecked
        Map<String, String> attributes = codec.treeToValue(tree.path("other"), Map.class);
        return new NodeBuilder()
                //TODO: fix attribute setting, because they override 'id' and 'classes' property
                .setAttributes(attributes)
                .setTag(tag)
                .setIndex(index)
                .setId(id)
                .addContent(innerText)
                .setClasses(classes)
                .build();
    }

    /**
     * @param by         page aware locator
     * @param targetPage the new HTML page source on which we should search for the element
     * @return a list of candidate locators, ordered by revelance, or empty list if was unable to heal
     */
    List<By> findNewLocations(PageAwareBy by, String targetPage) {
        List<Node> nodes = storage.getLastValidPath(by, by.getPageName());
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        return findNearest(nodes.toArray(new Node[0]), targetPage)
                .stream()
                .map(this::toLocator)
                .collect(Collectors.toList());
    }

    private By toLocator(Node node) {
        for (Set<SelectorComponent> detailLevel : selectorDetailLevels) {
            By locator = construct(node, detailLevel);
            List<WebElement> elements = webDriver.findElements(locator);
            if (elements.size() == 1) {
                return locator;
            }
        }
        throw new HealException();
    }

    private By construct(Node node, Set<SelectorComponent> detailLevel) {
        return By.cssSelector(detailLevel.stream()
                .map(component -> component.createComponent(node))
                .collect(Collectors.joining()));
    }

    /**
     * @param nodePath        the array of nodes which actually represent the full path of an element in HTML tree,
     *                        ordered from deepest to shallowest
     * @param destinationTree the HTML code of the current page
     * @return a list of nodes which are the candidates to be the searched element, ordered by relevance descending.
     */
    private List<Node> findNearest(Node[] nodePath, String destinationTree) {
        Node destination = parseTree(destinationTree);
        PathFinder pathFinder =
                new PathFinder(new LCSPathDistance(), new HeuristicNodeDistance());
        return pathFinder.find(new Path(nodePath), destination, recoveryTries);
    }

    private Node parseTree(String tree) {
        return new JsoupHTMLParser().parse(new ByteArrayInputStream(tree.getBytes(StandardCharsets.UTF_8)));
    }
}
