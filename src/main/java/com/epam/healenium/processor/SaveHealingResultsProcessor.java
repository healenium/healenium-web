package com.epam.healenium.processor;

import com.epam.healenium.model.HealingResult;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
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
        WebElement mainHealedElement = healingResult.getHealedElements().get(0).getElement();
        byte[] screenshot = captureScreen(mainHealedElement);
        healingResult.setScreenshot(screenshot);
        context.getElements().add(mainHealedElement);
    }

    protected byte[] captureScreen(WebElement element) {
        if (engine.isHealingBacklighted()) {
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("arguments[0].style.border='3px solid red'", element);
        }
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
