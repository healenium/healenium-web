package com.epam.healenium.processor;

import com.epam.healenium.model.ReferenceElementsDto;
import com.epam.healenium.treecomparing.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Healing Element Processor
 */
@Slf4j(topic = "healenium")
public class HealingProcessor extends BaseProcessor {

    public HealingProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public boolean validate() {
        ReferenceElementsDto referenceElementsDto = context.getReferenceElementsDto();
        if (referenceElementsDto.getPaths().isEmpty()) {
            log.warn("New element locator have not been found. There is no reference data to selector in the database." +
                    "\nSelector page: {}/selectors/ " +
                    "\nMake sure that: " +
                    "\n- Your locator was changed on the page and not in code." +
                    "\n- You ran successful tests.", engine.getClient().getServerUrl());
            throw context.getNoSuchElementException();
        }
        return true;
    }

    @Override
    public void execute() {
        String targetPage = engine.pageSource();
        Node destination = engine.parseTree(targetPage);
        context.setPageContent(targetPage);

        for (List<Node> nodes : context.getReferenceElementsDto().getPaths()) {
            healingService.findNewLocations(nodes, destination, context);
        }
    }

}
