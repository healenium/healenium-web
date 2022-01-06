package com.epam.healenium.processor;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.List;

/**
 * Find webElements from driver processor
 */
@Slf4j
public class FindElementsProcessor extends BaseProcessor {

    public FindElementsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public void execute() {
        List<WebElement> pageElements = driver.findElements(context.getPageAwareBy().getBy());
        if (pageElements.isEmpty()) {
            log.warn("Failed to find any elements using locator {}\nTrying to heal...", context.getPageAwareBy().getBy().toString());
        }
        pageElements.forEach(e -> context.getElementIds().add(((RemoteWebElement) e).getId()));
        context.setElements(pageElements);
    }
}
