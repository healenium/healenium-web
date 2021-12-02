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
import com.epam.healenium.model.HealingCandidateDto;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.MetricsDto;
import com.epam.healenium.treecomparing.HeuristicNodeDistance;
import com.epam.healenium.treecomparing.JsoupHTMLParser;
import com.epam.healenium.treecomparing.LCSPathDistance;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.NodeBuilder;
import com.epam.healenium.treecomparing.Path;
import com.epam.healenium.treecomparing.PathFinder;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.ResourceReader;
import com.epam.healenium.utils.StackUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.healenium.SelectorComponent.ATTRIBUTES;
import static com.epam.healenium.SelectorComponent.CLASS;
import static com.epam.healenium.SelectorComponent.ID;
import static com.epam.healenium.SelectorComponent.PARENT;
import static com.epam.healenium.SelectorComponent.PATH;
import static com.epam.healenium.SelectorComponent.POSITION;
import static com.epam.healenium.SelectorComponent.TAG;

@Slf4j
public class SelfHealingEngine {

    /**
     * A JavaScript source to extract an HTML item with its attributes
     */
    private static final String SCRIPT = ResourceReader.readResource(
            "itemsWithAttributes.js", s -> s.collect(Collectors.joining()));
    private static final Config DEFAULT_CONFIG = ConfigFactory.systemProperties().withFallback(
            ConfigFactory.load("healenium.properties").withFallback(ConfigFactory.load()));

    @Getter
    private final Config config;
    @Getter
    private final WebDriver webDriver;
    private final int recoveryTries;
    @Getter
    private final double scoreCap;
    private final List<Set<SelectorComponent>> selectorDetailLevels;
    @Getter
    private final RestClient client;
    @Getter
    private String healingTime;

    /**
     * @param delegate a delegate driver, not actually {@link SelfHealingDriver} instance.
     * @param config   user-defined configuration
     */
    public SelfHealingEngine(@NotNull WebDriver delegate, @NotNull Config config) {
        // merge given config with default values
        Config finalizedConfig = ConfigFactory.load(config).withFallback(DEFAULT_CONFIG);

        List<Set<SelectorComponent>> temp = new ArrayList<>();
        temp.add(EnumSet.of(TAG, ID));
        temp.add(EnumSet.of(TAG, CLASS));
        temp.add(EnumSet.of(PARENT, TAG, ID, CLASS));
        temp.add(EnumSet.of(PARENT, TAG, CLASS, POSITION));
        temp.add(EnumSet.of(PARENT, TAG, ID, CLASS, ATTRIBUTES));
        temp.add(EnumSet.of(PATH));

        this.webDriver = delegate;
        this.config = finalizedConfig;
        this.recoveryTries = finalizedConfig.getInt("recovery-tries");
        this.scoreCap = finalizedConfig.getDouble("score-cap");
        this.selectorDetailLevels = Collections.unmodifiableList(temp);
        this.client = new RestClient(finalizedConfig);
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
                .map(this::getNodePath)
                .collect(Collectors.toList());
        saveNodes(by, nodesToSave);
    }

    public void saveNodes(PageAwareBy key, List<List<Node>> elementsToSave) {
        client.selectorsRequest(key.getBy(), new ArrayList<>(elementsToSave));
    }

    public List<Node> getNodePath(WebElement webElement) {
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
        return path;
    }

    /**
     * Convert raw data to {@code Node}
     *
     * @param parser - JSON reader
     * @return path node
     * @throws IOException
     */
    private Node toNode(JsonParser parser) throws IOException {
        ObjectCodec codec = parser.getCodec();
        TreeNode tree = parser.readValueAsTree();
        String tag = codec.treeToValue(tree.path(FieldName.TAG), String.class);
        Integer index = codec.treeToValue(tree.path(FieldName.INDEX), Integer.class);
        String innerText = codec.treeToValue(tree.path(FieldName.INNER_TEXT), String.class);
        String id = codec.treeToValue(tree.path(FieldName.ID), String.class);
        //noinspection unchecked
        Set<String> classes = codec.treeToValue(tree.path(FieldName.CLASSES), Set.class);
        //noinspection unchecked
        Map<String, String> attributes = codec.treeToValue(tree.path(FieldName.OTHER), Map.class);
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
     * @param targetPage the new HTML page source on which we should search for the element
     * @param paths      source path to locator
     * @param metricsDto list of metrics data
     * @return a list of candidate locators, ordered by revelance, or empty list if was unable to heal
     */
    public List<Scored<By>> findNewLocations(String targetPage, List<Node> paths, MetricsDto metricsDto) {
        return findNearest(paths.toArray(new Node[0]), targetPage, metricsDto).stream()
                .map(this::toLocator)
                .collect(Collectors.toList());
    }


    private Scored<By> toLocator(Scored<Node> node) {
        for (Set<SelectorComponent> detailLevel : selectorDetailLevels) {
            By locator = construct(node.getValue(), detailLevel);
            List<WebElement> elements = webDriver.findElements(locator);
            if (elements.size() == 1) {
                return new Scored<>(node.getScore(), locator);
            }
        }
        throw new HealException();
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
    private List<Scored<Node>> findNearest(Node[] nodePath, String destinationTree, MetricsDto metricsDto) {
        final long then = System.currentTimeMillis();
        Node destination = parseTree(destinationTree);
        PathFinder pathFinder = new PathFinder(new LCSPathDistance(), new HeuristicNodeDistance());
        AbstractMap.SimpleImmutableEntry<Integer, Map<Double, List<AbstractMap.SimpleImmutableEntry<Node, Integer>>>> scoresToNodes =
                pathFinder.findScoresToNodes(new Path(nodePath), destination);
        List<Scored<Node>> scoreds = pathFinder.getSortedNodes(scoresToNodes.getValue(), recoveryTries, scoreCap);
        healingTime = String.valueOf((System.currentTimeMillis() - then) / 1000.0);
        collectMetrics(nodePath, scoresToNodes, scoreds, metricsDto);
        return scoreds;
    }

    private void collectMetrics(Node[] nodePath, AbstractMap.SimpleImmutableEntry<Integer, Map<Double,
            List<AbstractMap.SimpleImmutableEntry<Node, Integer>>>> curPathHeightToScores,
                                List<Scored<Node>> scoreds, MetricsDto metricsDto) {
        Integer curPathHeight = curPathHeightToScores.getKey();
        Map<Double, List<AbstractMap.SimpleImmutableEntry<Node, Integer>>> scoresToNodes = curPathHeightToScores.getValue();
        List<HealingCandidateDto> allHealingCandidates = scoresToNodes.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .flatMap(score -> scoresToNodes.get(score).stream()
                        .map(it -> new HealingCandidateDto(score, it.getValue(), curPathHeight, it.getKey())))
                .limit(10)
                .collect(Collectors.toList());
        if (scoreds.size() == 0) {
            return;
        }
        HealingCandidateDto mainHealingCandidate = allHealingCandidates.stream()
                .filter(candidate -> candidate.getScore().equals(scoreds.get(0).getScore())
                        && candidate.getNode().equals(scoreds.get(0).getValue()))
                .findFirst()
                .orElse(null);
        allHealingCandidates.remove(mainHealingCandidate);
        metricsDto.setTargetNode(new Path(nodePath).getLastNode())
                .setMainHealingCandidate(mainHealingCandidate)
                .setOtherHealingCandidates(allHealingCandidates);
    }

    private Node parseTree(String tree) {
        return new JsoupHTMLParser().parse(new ByteArrayInputStream(tree.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean isHealingEnabled() {
        boolean isDisabled = StackUtils.isAnnotationPresent(DisableHealing.class);
        return config.getBoolean("heal-enabled") && !isDisabled;
    }

    public boolean isHealingBacklighted() {
        return config.getBoolean("backlight-healing");
    }

}
