package com.epam.healenium.processor;

import com.epam.healenium.model.LastHealingDataDto;
import com.epam.healenium.treecomparing.Node;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Healing Elements Processor
 */
@Slf4j
public class HealingElementsProcessor extends BaseProcessor {

    public HealingElementsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public boolean validate() {
        LastHealingDataDto lastHealingData = context.getLastHealingData();
        if (lastHealingData == null || lastHealingData.getPaths().isEmpty()) {
            if (!context.getElements().isEmpty()) {
                engine.saveElements(context.getPageAwareBy(), context.getElements());
            } else {
                log.warn("New element locator have not been found. There is a lack of reference data.");
            }
            return false;
        }
        return true;
    }

    @Override
    public void execute() {
        List<List<Node>> nodesFromDb = context.getLastHealingData().getPaths();

        splitDbNodes(nodesFromDb);

        String targetPage = engine.pageSource();
        Node destination = engine.parseTree(targetPage);
        context.setPageContent(targetPage);

        nodesFromDb.stream()
                .filter(nodes -> !context.getNewElementsToNodes().containsValue(nodes) && !context.getExistElementsToNodes().containsValue(nodes))
                .forEach(nodes -> healingService.findNewLocations(nodes, destination, context));
    }

    private void splitDbNodes(List<List<Node>> nodesFromDb) {
        for (WebElement webElement : context.getElements()) {
            List<Node> nodePath = engine.getNodeService().getNodePath(webElement);
            if (!nodesFromDb.contains(nodePath)) {
                context.getNewElementsToNodes().put(webElement, nodePath);
            } else {
                context.getExistElementsToNodes().put(webElement, nodePath);
            }
        }
    }

}
