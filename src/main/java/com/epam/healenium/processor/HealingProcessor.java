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
                    "\nMake sure that: " +
                    "\n- There is selector on the page {}/selectors/ and type: single, if not then you have to run successful tests." +
                    "\n- Your locator was changed on the page and not in code.", engine.getClient().getServerUrl());
            throw context.getNoSuchElementException();
        }
        return true;
    }

    @Override
    public void execute() {
        String targetPage = engine.pageSource();
        Node destination = engine.parseTree(targetPage);
        context.setPageContent(targetPage);

        log.warn("Trying to heal...");
        for (List<Node> nodes : context.getReferenceElementsDto().getPaths()) {
            healingService.findNewLocations(nodes, destination, context, engine);
        }
    }

}
