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

import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingEngine;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.TargetLocator;

@AllArgsConstructor
class TargetLocatorProxyInvocationHandler implements InvocationHandler {

    private final TargetLocator delegate;
    private final SelfHealingEngine engine;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(delegate, args);
        boolean isProxy = result instanceof SelfHealingDriver;
        boolean isWebDriver = result instanceof WebDriver;
        if (isWebDriver && !isProxy) {
            return SelfHealingDriver.create(engine);
        } else {
            return result;
        }
    }
}
