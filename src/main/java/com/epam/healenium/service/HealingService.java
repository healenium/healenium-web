package com.epam.healenium.service;

import com.epam.healenium.SelectorComponent;
import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.model.*;
import com.epam.healenium.treecomparing.HeuristicNodeDistance;
import com.epam.healenium.treecomparing.LCSPathDistance;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Path;
import com.epam.healenium.treecomparing.PathFinder;
import com.epam.healenium.treecomparing.Scored;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j(topic = "healenium")
public class HealingService {

    private final int recoveryTries;
    private final double scoreCap;
    private final List<Set<SelectorComponent>> selectorDetailLevels;
    protected final WebDriver driver;

    private final static List<Set<SelectorComponent>> TEMP = new ArrayList<Set<SelectorComponent>>() {{
        add(EnumSet.of(SelectorComponent.TAG, SelectorComponent.ID));
        add(EnumSet.of(SelectorComponent.TAG, SelectorComponent.CLASS));
        add(EnumSet.of(SelectorComponent.PARENT, SelectorComponent.TAG, SelectorComponent.ID, SelectorComponent.CLASS));
        add(EnumSet.of(SelectorComponent.PARENT, SelectorComponent.TAG, SelectorComponent.CLASS, SelectorComponent.POSITION));
        add(EnumSet.of(SelectorComponent.PARENT, SelectorComponent.TAG, SelectorComponent.ID, SelectorComponent.CLASS, SelectorComponent.ATTRIBUTES));
        add(EnumSet.of(SelectorComponent.PATH));
    }};

    public HealingService(Config finalizedConfig, WebDriver driver) {
        this.recoveryTries = finalizedConfig.getInt("recovery-tries");
        this.scoreCap = finalizedConfig.getDouble("score-cap");
        this.selectorDetailLevels = Collections.unmodifiableList(TEMP);
        this.driver = driver;
    }

    /**
     * @param paths       source path to locator
     * @param destination the new HTML page source on which we should search for the element
     * @param context     context data for healing
     * @param engine
     */
    public void findNewLocations(List<Node> paths, Node destination, Context context, SelfHealingEngine engine) {
        PathFinder pathFinder = new PathFinder(new LCSPathDistance(), new HeuristicNodeDistance());
        AbstractMap.SimpleImmutableEntry<Integer, Map<Double, List<AbstractMap.SimpleImmutableEntry<Node, Integer>>>> scoresToNodes =
                pathFinder.findScoresToNodes(new Path(paths.toArray(new Node[0])), destination);
        List<Scored<Node>> scoreds = pathFinder.getSortedNodes(scoresToNodes.getValue(), 1000, scoreCap);

        List<HealedElement> healedElements = scoreds.stream()
                .map(node -> toLocator(node, context, engine))
                .filter(Objects::nonNull)
                .limit(recoveryTries)
                .collect(Collectors.toList());
        if (!healedElements.isEmpty()) {
            HealingResult healingResult = new HealingResult()
                    .setPaths(paths)
                    .setTargetNodes(scoreds)
                    .setAllHealingCandidates(getAllHealingCandidates(scoresToNodes))
                    .setHealedElements(healedElements);
            context.getHealingResults().add(healingResult);
        }
    }

    /**
     * @param node    convert source node to locator
     * @param context chain context
     * @param engine
     * @return healedElement
     */
    protected HealedElement toLocator(Scored<Node> node, Context context, SelfHealingEngine engine) {
        for (Set<SelectorComponent> detailLevel : selectorDetailLevels) {
            By locator = construct(node.getValue(), detailLevel);
            if (isUnsuccessLocator(locator, context, engine)) {
                return null;
            }
            List<WebElement> elements = driver.findElements(locator);
            if (elements.size() == 1 && !context.getElementIds().contains(((RemoteWebElement) elements.get(0)).getId()) ) {
                Scored<By> byScored = new Scored<>(node.getScore(), locator);
                context.getElementIds().add(((RemoteWebElement) elements.get(0)).getId());
                HealedElement healedElement = new HealedElement();
                healedElement.setElement(elements.get(0)).setScored(byScored);
                return healedElement;
            }
        }
        return null;
    }

    private boolean isUnsuccessLocator(By locator, Context context, SelfHealingEngine engine) {
        Locator convertLocator = engine.getClient().getMapper().byToLocator(locator);
        List<Locator> unsuccessfulLocators = context.getUnsuccessfulLocators();
        return unsuccessfulLocators != null && unsuccessfulLocators.contains(convertLocator);
    }

    /**
     * @param curPathHeightToScores - all PathToNode candidate collection
     * @return list healingCandidateDto for metrics
     */
    private List<HealingCandidateDto> getAllHealingCandidates(AbstractMap.SimpleImmutableEntry<Integer, Map<Double,
            List<AbstractMap.SimpleImmutableEntry<Node, Integer>>>> curPathHeightToScores) {
        Integer curPathHeight = curPathHeightToScores.getKey();
        Map<Double, List<AbstractMap.SimpleImmutableEntry<Node, Integer>>> scoresToNodes = curPathHeightToScores.getValue();
        return scoresToNodes.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .flatMap(score -> scoresToNodes.get(score).stream()
                        .map(it -> new HealingCandidateDto(score, it.getValue(), curPathHeight, it.getKey())))
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * construct cssSelector by Node
     *
     * @param node        - target node
     * @param detailLevel - final detail Level collection
     * @return target user selector
     */
    protected By construct(Node node, Set<SelectorComponent> detailLevel) {
        return By.cssSelector(detailLevel.stream()
                .map(component -> component.createComponent(node))
                .collect(Collectors.joining()));
    }
}
