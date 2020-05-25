/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.handlers.proxy;

import com.epam.healenium.PageAwareBy;
import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.service.HealingService;
import com.epam.healenium.service.impl.HealingServiceImpl;
import com.epam.healenium.utils.ProxyFactory;
import com.typesafe.config.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.InvocationHandler;

@Slf4j
public abstract class BaseHandler implements InvocationHandler {

    protected final SelfHealingEngine engine;
    protected final WebDriver driver;
    private final Config config;
    @Getter
    private final HealingService healingService;

    public BaseHandler(SelfHealingEngine engine) {
        this.engine = engine;
        this.driver = engine.getWebDriver();
        this.config = engine.getConfig();
        this.healingService = new HealingServiceImpl(engine);
    }

    protected WebElement findElement(By by) {
        try {
            PageAwareBy pageBy = awareBy(by);
            By inner = pageBy.getBy();
            if (engine.isHealingEnabled()) {
                return lookUp(pageBy);
            }
            return driver.findElement(inner);
        } catch (Exception ex) {
            throw new NoSuchElementException("Failed to find element using " + by.toString(), ex);
        }
    }

    /**
     * Search target element on a page
     * @param key will be used for checking|saving in cache
     * @return proxy web element
     */
    protected WebElement lookUp(PageAwareBy key) {
        try {
            WebElement element = driver.findElement(key.getBy());
            engine.savePath(key, element);
            return element;
        } catch (NoSuchElementException e) {
            log.warn("Failed to find an element using locator {}\nReason: {}\nTrying to heal...", key.getBy().toString(), e.getMessage());
            return healingService.heal(key, e).orElseThrow(() -> e);
        }
    }

    /**
     *
     * @param by
     * @return
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

}
