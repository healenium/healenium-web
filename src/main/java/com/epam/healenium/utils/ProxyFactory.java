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
package com.epam.healenium.utils;

import com.epam.healenium.SelfHealingDriver;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.HasInputDevices;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.internal.WrapsElement;

@UtilityClass
@SuppressWarnings("unchecked")
public class ProxyFactory {

    public static <T extends WebDriver> SelfHealingDriver createDriverProxy(ClassLoader loader, InvocationHandler handler, Class<T> clazz) {
        Class<?>[] interfaces = Stream.concat(
            Arrays.stream(clazz.getInterfaces()),
            Stream.of(JavascriptExecutor.class, SelfHealingDriver.class, HasInputDevices.class, Interactive.class)
        ).distinct().toArray(Class[]::new);

        return (SelfHealingDriver) Proxy.newProxyInstance(loader, interfaces, handler);
    }

    public static <T extends WebElement> T createWebElementProxy(ClassLoader loader, InvocationHandler handler) {
        Class<?>[] interfaces = new Class[]{WebElement.class, WrapsElement.class, Locatable.class};
        return (T) Proxy.newProxyInstance(loader, interfaces, handler);
    }

    public static <T extends WebDriver.TargetLocator> T createTargetLocatorProxy(ClassLoader loader, InvocationHandler handler) {
        Class<?>[] interfaces = new Class[]{WebDriver.TargetLocator.class};
        return (T) Proxy.newProxyInstance(loader, interfaces, handler);
    }

}
