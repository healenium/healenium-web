package com.epam.healenium.processor;

import com.epam.healenium.model.LastHealingDataDto;
import com.epam.healenium.treecomparing.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Healing Element Processor
 */
@Slf4j
public class HealingProcessor extends BaseProcessor {

    public HealingProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public boolean validate() {
        LastHealingDataDto lastHealingDataDto = context.getLastHealingData();
        if (lastHealingDataDto.getPaths().isEmpty()) {
            log.warn("New element locator have not been found. There is a lack of reference data.");
            throw context.getNoSuchElementException();
        }
        return true;
    }

    @Override
    public void execute() {
        String targetPage = engine.pageSource();
        Node destination = engine.parseTree(targetPage);
        context.setPageContent(targetPage);

        for (List<Node> nodes : context.getLastHealingData().getPaths()) {
            healingService.findNewLocations(nodes, destination, context);
        }
    }

}
