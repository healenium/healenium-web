package com.epam.healenium.processor;

import com.epam.healenium.model.HealedElement;
import com.epam.healenium.model.HealingResult;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;

/**
 * Save Healing Results Processor
 */
@Slf4j
public class SaveHealingResultsProcessor extends BaseProcessor {

    public SaveHealingResultsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public void execute() {
        context.getHealingResults()
                .forEach(this::enrichHealingResult);
        if (!context.getHealingResults().isEmpty()) {
            restClient.healRequest(context);
        }
    }

    public void enrichHealingResult(HealingResult healingResult) {
        HealedElement mainCandidate = healingResult.getHealedElements().get(0);
        WebElement mainHealedElement = mainCandidate.getElement();
        log.warn("Using healed locator: {}", mainCandidate.getScored());
        byte[] screenshot = engine.captureScreen(mainHealedElement);
        healingResult.setScreenshot(screenshot);
        context.getElements().add(mainHealedElement);
    }

}
