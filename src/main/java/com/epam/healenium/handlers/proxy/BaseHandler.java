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
import com.epam.healenium.handlers.SelfHealingHandler;
import com.epam.healenium.model.Context;
import com.epam.healenium.processor.BaseProcessor;
import com.epam.healenium.utils.ProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

@Slf4j(topic = "healenium")
public class BaseHandler implements SelfHealingHandler {

    protected final SelfHealingEngine engine;
    protected final WebDriver driver;
    protected BaseProcessor findElementChainProcessor;
    protected BaseProcessor findElementsChainProcessor;

    public BaseHandler(SelfHealingEngine engine) {
        this.engine = engine;
        this.driver = engine.getWebDriver();
        this.findElementChainProcessor = ProcessorConfig.findElementChainProcessor();
        this.findElementsChainProcessor = ProcessorConfig.findElementsChainProcessor();
    }

    /**
     * Search target element on a page
     *
     * @param by will be used for checking|saving in cache
     * @return proxy web element
     */
    @Override
    public WebElement findElement(By by) {
        try {
            if (engine.getSessionContext().isWaitCommand()) {
                engine.getSessionContext().setFindElementWaitCommand(true);
            }
            if (engine.isHealingEnabled()) {
                Context context = new Context()
                        .setBy(by)
                        .setAction("findElement");

                setBaseProcessorFields(findElementChainProcessor, context);
                findElementChainProcessor.process();

                if (context.getElements().size() > 0) {
                    return context.getElements().get(0);
                }
                if (context.getNoSuchElementException() != null) {
                    throw context.getNoSuchElementException();
                } else {
                    throw new NoSuchElementException("Failed to find element using " + by.toString());
                }
            }
            return driver.findElement(by);
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
    @Override
    public List<WebElement> findElements(By by) {
        try {
            if (engine.getSessionContext().isWaitCommand()) {
                engine.getSessionContext().setFindElementWaitCommand(true);
            }
            if (engine.isHealingEnabled()) {
                Context context = new Context()
                        .setBy(by)
                        .setAction("findElements");
                setBaseProcessorFields(findElementsChainProcessor, context);
                findElementsChainProcessor.process();

                return context.getElements();
            }
            return driver.findElements(by);
        } catch (Exception ex) {
            throw new NoSuchElementException("Failed to find elements using " + by.toString(), ex);
        }
    }


    /**
     * @param by locator
     * @return PageAwareBy element
     */
    protected PageAwareBy awareBy(By by) {
        return (by instanceof PageAwareBy) ? (PageAwareBy) by : PageAwareBy.by(null, by);
    }

    @Override
    public WebElement wrapElement(WebElement element, ClassLoader loader) {
        WebElementProxyHandler elementProxyHandler = new WebElementProxyHandler(element, engine);
        return ProxyFactory.createWebElementProxy(loader, elementProxyHandler);
    }

    @Override
    public WebDriver.TargetLocator wrapTarget(WebDriver.TargetLocator locator, ClassLoader loader) {
        TargetLocatorProxyInvocationHandler handler = new TargetLocatorProxyInvocationHandler(locator, engine);
        return ProxyFactory.createTargetLocatorProxy(loader, handler);
    }

    @Override
    public void quit() {
        engine.quit();
    }

    protected void setBaseProcessorFields(BaseProcessor baseProcessor, Context context) {
        baseProcessor.setContext(context)
                .setDriver(driver)
                .setEngine(engine)
                .setRestClient(engine.getClient())
                .setHealingService(engine.getHealingService());
    }

}
