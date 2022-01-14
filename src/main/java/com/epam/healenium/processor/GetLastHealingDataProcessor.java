package com.epam.healenium.processor;

import com.epam.healenium.model.LastHealingDataDto;
import com.epam.healenium.model.Locator;
import lombok.extern.slf4j.Slf4j;

/**
 * Get Last Healing Data processor to heal element
 */
@Slf4j
public class GetLastHealingDataProcessor extends BaseProcessor {

    public GetLastHealingDataProcessor(BaseProcessor nextProcessor) {
        super(nextProcessor);
    }

    @Override
    public boolean validate() {
        return context.getNoSuchElementException() != null || "findElements".equals(context.getAction());
    }

    @Override
    public void execute() {
        String currentUrl = engine.getCurrentUrl();
        LastHealingDataDto lastHealingDataDto = restClient.getLastHealingData(
                context.getPageAwareBy().getBy(), currentUrl).orElse(null);
        context.setLastHealingData(lastHealingDataDto);
        context.setCurrentUrl(currentUrl);
        Locator userLocator = mapper.byToLocator(context.getPageAwareBy().getBy());
        context.setUserLocator(userLocator);
    }
}
