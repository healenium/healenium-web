package com.epam.healenium.processor;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.List;

/**
 * Find child webElements from parent webElement processor
 */
@Slf4j(topic = "healenium")
public class FindChildElementsProcessor extends BaseProcessor {

    public FindChildElementsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public void execute() {
        List<WebElement> pageElements = delegateElement.findElements(context.getBy());
        pageElements.forEach(e -> context.getElementIds().add(((RemoteWebElement) e).getId()));
        engine.saveElements(context, pageElements);
        context.setElements(pageElements);
    }
}
