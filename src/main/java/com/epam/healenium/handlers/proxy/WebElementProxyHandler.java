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
import com.epam.healenium.model.Context;
import com.epam.healenium.processor.BaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j(topic = "healenium")
public class WebElementProxyHandler extends BaseHandler implements InvocationHandler {

    private WebElement delegate;

    public WebElementProxyHandler(WebElement delegate, SelfHealingEngine engine) {
        super(engine);
        this.delegate = delegate;
        this.findElementChainProcessor = ProcessorConfig.findChildElementChainProcessor();
        this.findElementsChainProcessor = ProcessorConfig.findChildElementsChainProcessor();
    }

    public WebElementProxyHandler(SelfHealingEngine engine) {
        super(engine);
        this.findElementChainProcessor = ProcessorConfig.findChildElementChainProcessor();
        this.findElementsChainProcessor = ProcessorConfig.findChildElementsChainProcessor();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            ClassLoader loader = driver.getClass().getClassLoader();
            if ("findElement".equals(method.getName())) {
                WebElement element = findElement((By) args[0]);
                return engine.isProxy()
                        ? element
                        : Optional.ofNullable(element).map(it -> wrapElement(it, loader)).orElse(null);
            }
            if ("findElements".equals(method.getName())) {
                List<WebElement> elements = findElements((By) args[0]);
                return engine.isProxy()
                        ? elements
                        : elements.stream().map(it -> wrapElement(it, loader)).collect(Collectors.toList());
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
    public WebElement findElement(By by) {
        try {
            PageAwareBy pageBy = awareBy(by);
            if (engine.isHealingEnabled()) {
                Context context = new Context()
                        .setBy(pageBy)
                        .setAction("findElement");
                setBaseProcessorFields(findElementChainProcessor, context);
                findElementChainProcessor.process();

                if (context.getElements().size() > 0) {
                    return context.getElements().get(0);
                }
                throw context.getNoSuchElementException();
            }
            return delegate.findElement(pageBy.getBy());
        } catch (Exception ex) {
            throw new NoSuchElementException("Failed to find element using " + by.toString(), ex);
        }
    }

    @Override
    public List<WebElement> findElements(By by) {
        try {
            PageAwareBy pageBy = awareBy(by);
            By inner = pageBy.getBy();
            if (engine.isHealingEnabled()) {
                Context context = new Context()
                        .setBy(pageBy)
                        .setAction("findElements");
                setBaseProcessorFields(findElementsChainProcessor, context);
                findElementsChainProcessor.process();

                return context.getElements();
            }
            return delegate.findElements(inner);
        } catch (Exception ex) {
            throw new NoSuchElementException("Failed to find elements using " + by.toString(), ex);
        }
    }

    protected void setBaseProcessorFields(BaseProcessor baseProcessor, Context context) {
        super.setBaseProcessorFields(baseProcessor, context);
        baseProcessor.setDelegateElement(delegate);
    }

    public WebElementProxyHandler setDelegate(WebElement delegate) {
        this.delegate = delegate;
        return this;
    }
}
