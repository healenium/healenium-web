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
import java.lang.reflect.Method;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

@Slf4j
public class WebElementProxyHandler extends BaseHandler {

    private final WebElement delegate;

    public WebElementProxyHandler(WebElement delegate, SelfHealingEngine engine) {
        super(engine);
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            ClassLoader loader = driver.getClass().getClassLoader();
            if ("findElement".equals(method.getName())) {
                WebElement element = findElement((By) args[0]);
                return Optional.ofNullable(element).map(it -> wrapElement(it, loader)).orElse(null);
            }
            if ("getWrappedElement".equals(method.getName())) {
                return delegate;
            }
            return method.invoke(delegate, args);
        } catch (Exception ex) {
            throw ex.getCause();
        }
    }

    @Override
    protected WebElement findElement(By by) {
        try {
            PageAwareBy pageBy = awareBy(by);
            if (engine.isHealingEnabled()) {
                return lookUp(pageBy);
            }
            return delegate.findElement(pageBy.getBy());
        } catch (Exception ex) {
            throw new NoSuchElementException("Failed to find element using " + by.toString(), ex);
        }

    }

    @Override
    protected WebElement lookUp(PageAwareBy key) {
        try {
            WebElement element = delegate.findElement(key.getBy());
            engine.savePath(key, element);
            return element;
        } catch (NoSuchElementException ex) {
            log.warn("Failed to find an element using locator {}\nReason: {}\nTrying to heal...", key.getBy().toString(), ex.getMessage());
            return getHealingService().heal(key, ex).orElseThrow(() -> ex);
        }
    }
}
