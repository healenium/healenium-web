package com.epam.healenium.service.impl;

import com.epam.healenium.PageAwareBy;
import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.model.LastHealingDataDto;
import com.epam.healenium.service.HealingElementsService;
import com.epam.healenium.treecomparing.Node;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class HealingElementsServiceImpl extends AbstractHealingServiceImpl implements HealingElementsService {

    public HealingElementsServiceImpl(SelfHealingEngine engine) {
        super(engine);
    }

    @Override
    public List<WebElement> heal(PageAwareBy pageBy, List<WebElement> pageElements) {
        Optional<LastHealingDataDto> lastHealingDataDto = getLastHealingDataDto(pageBy);
        if (!lastHealingDataDto.isPresent() || lastHealingDataDto.get().getPaths().isEmpty()) {
            return pageElements;
        }
        List<List<Node>> lastValidPath = new ArrayList<>(lastHealingDataDto.map(LastHealingDataDto::getPaths).get());
        if (lastValidPath.isEmpty() && !pageElements.isEmpty()) {
            engine.saveElements(pageBy, pageElements);
            return pageElements;
        } else {
            return healAndSave(pageBy, pageElements, lastValidPath, lastHealingDataDto);
        }
    }

    private List<WebElement> healAndSave(PageAwareBy pageBy,
                                         List<WebElement> elementsFromPage,
                                         List<List<Node>> nodesFromDb,
                                         Optional<LastHealingDataDto> lastHealingDataDto) {
        Map<WebElement, List<Node>> elementToNodeFromPage = new HashMap<>();
        List<List<Node>> nodesFromDbToDelete = new ArrayList<>();
        elementsFromPage.forEach(e -> elementToNodeFromPage.put(e, new ArrayList<>(engine.getNodePath(e))));

        nodesFromDb.forEach(node -> {
            elementToNodeFromPage.entrySet().removeIf(entry -> {
                if (node.equals(entry.getValue())) {
                    nodesFromDbToDelete.add(node);
                    return true;
                }
                return false;
            });
        });
        nodesFromDb.removeAll(nodesFromDbToDelete);

        List<WebElement> healedElements = nodesFromDb.stream()
                .map(nodes -> {
                    log.warn("Failed to find an element using locator {}\nTrying to heal...", pageBy.getBy());
                    return healLocator(pageBy, nodes, lastHealingDataDto).orElse(null);
                })
                .map(driver::findElement)
                .collect(Collectors.toList());
        addHealedElements(elementsFromPage, elementToNodeFromPage, healedElements);

        if (!elementToNodeFromPage.isEmpty()) {
            List<List<Node>> nodesToSave = lastHealingDataDto.get().getPaths();
            nodesToSave.addAll(elementToNodeFromPage.values());
            engine.saveNodes(pageBy, nodesToSave);
        }
        return elementsFromPage;
    }

    private void addHealedElements(List<WebElement> elementsFromPage, Map<WebElement, List<Node>> elementToNodeFromPage, List<WebElement> healedElements) {
        if (!healedElements.isEmpty()) {
            elementsFromPage.addAll(healedElements);
            List<List<Node>> healedNodes = healedElements.stream()
                    .map(engine::getNodePath)
                    .collect(Collectors.toList());
            if (!elementToNodeFromPage.isEmpty()) {
                healedNodes.forEach(healedNode -> elementToNodeFromPage.entrySet()
                        .removeIf(entry -> healedNode.equals(entry.getValue())));
            }
        }
    }
}
