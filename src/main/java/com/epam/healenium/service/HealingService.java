package com.epam.healenium.service;

import com.epam.healenium.SelectorComponent;
import com.epam.healenium.model.Context;
import com.epam.healenium.model.HealedElement;
import com.epam.healenium.model.HealingCandidateDto;
import com.epam.healenium.model.HealingResult;
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

@Slf4j
public class HealingService {

    private final int recoveryTries;
    private final double scoreCap;
    private final List<Set<SelectorComponent>> selectorDetailLevels;
    private final WebDriver driver;

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
     * @param destination the new HTML page source on which we should search for the element
     * @param paths       source path to locator
     * @param context     context data for healing
     */
    public void findNewLocations(List<Node> paths, Node destination, Context context) {

        final long then = System.currentTimeMillis();
        PathFinder pathFinder = new PathFinder(new LCSPathDistance(), new HeuristicNodeDistance());
        AbstractMap.SimpleImmutableEntry<Integer, Map<Double, List<AbstractMap.SimpleImmutableEntry<Node, Integer>>>> scoresToNodes =
                pathFinder.findScoresToNodes(new Path(paths.toArray(new Node[0])), destination);
        List<Scored<Node>> scoreds = pathFinder.getSortedNodes(scoresToNodes.getValue(), recoveryTries, scoreCap);

        List<HealedElement> healedElements = scoreds.stream()
                .map(node -> toLocator(node, context))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        String healingTime = String.valueOf((System.currentTimeMillis() - then) / 1000.0);
        if (!healedElements.isEmpty()) {
            HealingResult healingResult = new HealingResult()
                    .setPaths(paths)
                    .setTargetNodes(scoreds)
                    .setAllHealingCandidates(getAllHealingCandidates(scoresToNodes))
                    .setHealingTime(healingTime)
                    .setHealedElements(healedElements);
            context.getHealingResults().add(healingResult);
        }
    }

    /**
     *
     * @param node    convert source node to locator
     * @param context chain context
     * @return healedElement
     */
    private HealedElement toLocator(Scored<Node> node, Context context) {
        for (Set<SelectorComponent> detailLevel : selectorDetailLevels) {
            By locator = construct(node.getValue(), detailLevel);
            List<WebElement> elements = driver.findElements(locator);
            if (elements.size() == 1 && !context.getElementIds().contains(((RemoteWebElement) elements.get(0)).getId())) {
                Scored<By> byScored = new Scored<>(node.getScore(), locator);
                context.getElementIds().add(((RemoteWebElement) elements.get(0)).getId());
                HealedElement healedElement = new HealedElement();
                healedElement.setElement(elements.get(0)).setScored(byScored);
                return healedElement;
            }
        }
        return null;
    }

    /**
     *
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
     * @param node         - target node
     * @param detailLevel  - final detail Level collection
     * @return target user selector
     */
    private By construct(Node node, Set<SelectorComponent> detailLevel) {
        return By.cssSelector(detailLevel.stream()
                .map(component -> component.createComponent(node))
                .collect(Collectors.joining()));
    }
}
