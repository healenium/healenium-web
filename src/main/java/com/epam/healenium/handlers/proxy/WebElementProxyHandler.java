package com.epam.healenium.handlers.proxy;

import com.epam.healenium.SelfHealingEngine;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
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
        if ("findElement".equals(method.getName())) {
            log.debug("Caught findElement: invoking the healing version...");
            return findElement((By) args[0]);
        }
        if ("getWrappedElement".equals(method.getName())) {
            return delegate;
        }
        return method.invoke(delegate, args);
    }
}
