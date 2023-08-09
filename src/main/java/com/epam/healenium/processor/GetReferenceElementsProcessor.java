package com.epam.healenium.processor;

import com.epam.healenium.model.Locator;
import com.epam.healenium.model.ReferenceElementsDto;
import com.epam.healenium.utils.SystemUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;

/**
 * Get Last Healing Data processor to heal element
 */
@Slf4j(topic = "healenium")
public class GetReferenceElementsProcessor extends BaseProcessor {

    public GetReferenceElementsProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public boolean validate() {
        if (engine.getSessionContext().isWaitCommand()) {
            return false;
        }
        Locator locator = engine.getClient().getMapper().byToLocator(context.getBy());
        Map<String, String> enableHealingSelectors = engine.getSessionContext().getEnableHealingElements();
        Map<String, String> disableHealingSelector = engine.getSessionContext().getDisableHealingElement();
        String fullLocator = locator.getType() + locator.getValue();
        if (disableHealingSelector.containsValue(fullLocator) && "findElement".equals(context.getAction())) {
            boolean healingEnabled = isContains(disableHealingSelector);
            if (healingEnabled) {
                return false;
            }
        }
        if (enableHealingSelectors.containsValue(fullLocator) && !"findElement".equals(context.getAction())) {
            boolean healingEnabled = isContains(enableHealingSelectors);
            if (healingEnabled) {
                return true;
            }
        }
        if ("findElement".equals(context.getAction())) {
            return context.getNoSuchElementException() != null;
        }
        return false;
    }

    @Override
    public void execute() {
        log.warn("Failed to find an element using locator {}", context.getBy().toString());
        if (context.getNoSuchElementException() != null) {
            log.warn("Reason: {}", context.getNoSuchElementException().getMessage());
        }
        log.warn("Trying to heal...");
        populateUrlKey();
        ReferenceElementsDto referenceElementsDto = restClient.getReferenceElements(
                        context.getBy(), context.getAction(), context.getCurrentUrl())
                .orElse(new ReferenceElementsDto().setPaths(new ArrayList<>()));
        context.setReferenceElementsDto(referenceElementsDto);
        context.setUnsuccessfulLocators(referenceElementsDto.getUnsuccessfulLocators());
        Locator userLocator = restClient.getMapper().byToLocator(context.getBy());
        context.setUserLocator(userLocator);
    }

    private boolean isContains(Map<String, String> enableHealingSelectors) {
        populateUrlKey();
        String[] locatorParts = restClient.getMapper().getLocatorParts(context.getBy());
        String selectorId = SystemUtils.getMd5Hash(locatorParts[1].trim(), context.getAction(), context.getUrlKey());
        return enableHealingSelectors.containsKey(selectorId);
    }

    private void populateUrlKey() {
        if (context.getUrlKey() == null) {
            if (context.getCurrentUrl() == null) {
                context.setCurrentUrl(engine.getCurrentUrl());
            }
            String urlKey = engine.getSessionContext().getFunctionUrl().apply(engine, context.getCurrentUrl());
            log.debug("[Find Element] Get reference element. UrlKey: {}",  urlKey);
            context.setUrlKey(urlKey);
        }
    }

}
