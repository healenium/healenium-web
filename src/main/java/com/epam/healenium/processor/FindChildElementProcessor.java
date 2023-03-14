package com.epam.healenium.processor;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.Collections;

/**
 * Find child webElement from parent webElement processor
 */
@Slf4j(topic = "healenium")
public class FindChildElementProcessor extends BaseProcessor {

    public FindChildElementProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public void execute() {
        try {
            WebElement element = delegateElement.findElement(context.getBy());
            context.getElementIds().add(((RemoteWebElement) element).getId());
            engine.saveElements(context, Collections.singletonList(element));
            context.getElements().add(element);
        } catch (NoSuchElementException e) {
            context.setNoSuchElementException(e);
        }
    }
}
