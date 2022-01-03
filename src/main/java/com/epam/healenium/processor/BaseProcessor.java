package com.epam.healenium.processor;

import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.client.RestClient;
import com.epam.healenium.handlers.processor.ProcessorHandler;
import com.epam.healenium.mapper.HealeniumMapper;
import com.epam.healenium.model.Context;
import com.epam.healenium.service.HealingService;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

@Slf4j
@Accessors(chain = true)
public abstract class BaseProcessor implements ProcessorHandler {

    protected BaseProcessor nextProcessor;
    @Setter
    protected Context context;
    @Setter
    protected SelfHealingEngine engine;
    @Setter
    protected RestClient restClient;
    @Setter
    protected WebDriver driver;
    @Setter
    protected HealeniumMapper mapper;
    @Setter
    protected WebElement delegateElement;
    @Setter
    protected HealingService healingService;

    public BaseProcessor() {
    }

    public BaseProcessor(BaseProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    public void process() {
        if (validate()) {
            final long then = System.currentTimeMillis();
            execute();
            System.out.println("processor " + this.getClass().getName() + "  finish: " + ((System.currentTimeMillis() - then) / 1000.0));
            if (nextProcessor != null) {
                nextProcessor.setContext(context)
                        .setDriver(driver)
                        .setEngine(engine)
                        .setMapper(mapper)
                        .setRestClient(restClient)
                        .setHealingService(healingService)
                        .process();
            }
        }
    }

    public boolean validate() {
        return true;
    }

    public abstract void execute();
}
