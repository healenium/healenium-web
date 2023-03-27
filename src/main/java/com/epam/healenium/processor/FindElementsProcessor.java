package com.epam.healenium.processor;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.List;

/**
 * Find webElements from driver processor
 */
@Slf4j(topic = "healenium")
public class FindElementsProcessor extends BaseProcessor {

    public FindElementsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public void execute() {
        List<WebElement> pageElements = driver.findElements(context.getBy());
        pageElements.forEach(e -> context.getElementIds().add(((RemoteWebElement) e).getId()));
        engine.saveElements(context, pageElements);
        context.setElements(pageElements);
    }
}
