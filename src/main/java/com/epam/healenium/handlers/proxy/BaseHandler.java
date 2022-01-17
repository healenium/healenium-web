/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.handlers.proxy;

import com.epam.healenium.PageAwareBy;
import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.config.ProcessorConfig;
import com.epam.healenium.mapper.HealeniumMapper;
import com.epam.healenium.model.Context;
import com.epam.healenium.processor.BaseProcessor;
import com.epam.healenium.utils.ProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.InvocationHandler;
import java.util.List;

@Slf4j
public abstract class BaseHandler implements InvocationHandler {

    protected final SelfHealingEngine engine;
    protected final WebDriver driver;
    protected final ProcessorConfig processorConfig;

    public BaseHandler(SelfHealingEngine engine) {
        this.engine = engine;
        this.driver = engine.getWebDriver();
        this.processorConfig = new ProcessorConfig();
    }

    /**
     * Search target element on a page
     *
     * @param by will be used for checking|saving in cache
     * @return proxy web element
     */
    protected WebElement findElement(By by) {
        try {
            PageAwareBy pageBy = awareBy(by);
            By inner = pageBy.getBy();
            if (engine.isHealingEnabled()) {
                Context context = new Context()
                        .setPageAwareBy(pageBy)
                        .setAction("findElement");
                BaseProcessor chainProcessor = processorConfig.findElementChainProcessor();
                setBaseProcessorFields(chainProcessor, context);
                chainProcessor.process();

                return context.getElements().get(0);
            }
            return driver.findElement(inner);
        } catch (NoSuchElementException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Error during findElement: ", ex);
            throw new NoSuchElementException("Failed to find element using " + by.toString(), ex);
        }
    }

    /**
     * Search target elements on a page
     *
     * @param by will be used for checking|saving in cache
     * @return proxy web elements
     */
    protected List<WebElement> findElements(By by) {
        try {
            PageAwareBy pageBy = awareBy(by);
            By inner = pageBy.getBy();
            if (engine.isHealingEnabled()) {
                Context context = new Context()
                        .setPageAwareBy(pageBy)
                        .setAction("findElements");
                BaseProcessor chainProcessor = processorConfig.findElementsChainProcessor();
                setBaseProcessorFields(chainProcessor, context);
                chainProcessor.process();

                return context.getElements();
            }
            return driver.findElements(inner);
        } catch (Exception ex) {
            throw new NoSuchElementException("Failed to find elements using " + by.toString(), ex);
        }
    }


    /**
     * @param by locator
     * @return PageAwareBy element
     */
    protected PageAwareBy awareBy(By by) {
        return (by instanceof PageAwareBy) ? (PageAwareBy) by : PageAwareBy.by(driver.getTitle(), by);
    }

    protected WebElement wrapElement(WebElement element, ClassLoader loader) {
        WebElementProxyHandler elementProxyHandler = new WebElementProxyHandler(element, engine);
        return ProxyFactory.createWebElementProxy(loader, elementProxyHandler);
    }

    protected WebDriver.TargetLocator wrapTarget(WebDriver.TargetLocator locator, ClassLoader loader) {
        TargetLocatorProxyInvocationHandler handler = new TargetLocatorProxyInvocationHandler(locator, engine);
        return ProxyFactory.createTargetLocatorProxy(loader, handler);
    }

    protected void setBaseProcessorFields(BaseProcessor baseProcessor, Context context) {
        baseProcessor.setContext(context)
                .setDriver(driver)
                .setEngine(engine)
                .setRestClient(engine.getClient())
                .setMapper(new HealeniumMapper())
                .setHealingService(engine.getHealingService());
    }

}
