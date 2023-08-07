package com.epam.healenium.processor;

import com.epam.healenium.model.ReferenceElementsDto;
import com.epam.healenium.treecomparing.Node;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Healing Elements Processor
 */
@Slf4j(topic = "healenium")
public class HealingElementsProcessor extends BaseProcessor {

    public HealingElementsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public boolean validate() {
        ReferenceElementsDto lastHealingData = context.getReferenceElementsDto();
        if (lastHealingData == null || lastHealingData.getPaths().isEmpty()) {
            if (context.getElements().isEmpty()) {
                log.warn("New element locator have not been found. There is no reference data to selector in the database." +
                        "\nMake sure that: " +
                        "\n- There is selector on the page {}/selectors/ and type: multiple, if not then you have to run successful tests." +
                        "\n- Your locator was changed on the page and not in code.", engine.getClient().getServerUrl());
            }
            return false;
        }
        return true;
    }

    @Override
    public void execute() {
        List<List<Node>> nodesFromDb = context.getReferenceElementsDto().getPaths();

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
            List<Node> nodePath = engine.getNodeService().getNodePath(driver, webElement, context);
            if (!nodesFromDb.contains(nodePath)) {
                context.getNewElementsToNodes().put(webElement, nodePath);
            } else {
                context.getExistElementsToNodes().put(webElement, nodePath);
            }
        }
    }

}
