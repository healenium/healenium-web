package com.epam.healenium.processor;

import com.epam.healenium.PageAwareBy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.Collections;

@Slf4j
public class FindChildElementProcessor extends BaseProcessor {

    public FindChildElementProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public void execute() {
        try {
            PageAwareBy key = context.getPageAwareBy();
            WebElement element = delegateElement.findElement(key.getBy());
            engine.saveElements(key, Collections.singletonList(element));
            context.getElements().add(element);
        } catch (NoSuchElementException e) {
            log.warn("Failed to find an element using locator {}\nReason: {}\nTrying to heal...",
                    context.getPageAwareBy().getBy().toString(), e.getMessage());
            context.setNoSuchElementException(e);
        }
    }
}
