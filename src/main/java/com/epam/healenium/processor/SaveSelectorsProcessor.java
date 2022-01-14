package com.epam.healenium.processor;

import com.epam.healenium.treecomparing.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * Save Healing Results Processor
 */
@Slf4j
public class SaveSelectorsProcessor extends BaseProcessor {

    public SaveSelectorsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public void execute() {
        List<List<Node>> nodesFromDb = context.getLastHealingData().getPaths();
        Collection<List<Node>> newNodes = context.getNewElementsToNodes().values();
        if (!newNodes.isEmpty()) {
            nodesFromDb.addAll(newNodes);
            restClient.selectorsRequest(context.getPageAwareBy().getBy(),
                    nodesFromDb, context.getCurrentUrl());
        }
    }
}
