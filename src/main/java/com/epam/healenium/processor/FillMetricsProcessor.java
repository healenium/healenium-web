package com.epam.healenium.processor;

import com.epam.healenium.model.HealingResult;
import com.epam.healenium.model.HealingCandidateDto;
import com.epam.healenium.model.MetricsDto;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Path;
import lombok.extern.slf4j.Slf4j;

/**
 * Collect metrics for each healing result processor
 */
@Slf4j(topic = "healenium")
public class FillMetricsProcessor extends BaseProcessor {

    public FillMetricsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public void execute() {
        for (HealingResult healingResult : context.getHealingResults()) {
            HealingCandidateDto mainHealingCandidate = healingResult.getAllHealingCandidates().stream()
                    .findFirst()
                    .orElse(null);
            healingResult.getAllHealingCandidates().remove(mainHealingCandidate);

            healingResult.setMetricsDto(new MetricsDto()
                    .setCurrentDom(context.getPageContent())
                    .setUserSelector(context.getUserLocator())
                    .setTargetNode(new Path(healingResult.getPaths().toArray(new Node[0])).getLastNode())
                    .setMainHealingCandidate(mainHealingCandidate)
                    .setOtherHealingCandidates(healingResult.getAllHealingCandidates()));
        }
    }
}
