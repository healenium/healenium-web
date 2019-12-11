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
package com.epam.healenium;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.stream.Stream;

public interface SelfHealingDriver extends WebDriver {

    SelfHealingEngine getCurrentEngine();

    <T extends WebDriver> T getDelegate();

    /**
     * Instantiates the self-healing driver.
     *
     * @param delegate the original driver, like {@link org.openqa.selenium.chrome.ChromeDriver}, {@link
     *                 org.openqa.selenium.firefox.FirefoxDriver}, etc.
     */
    static SelfHealingDriver create(WebDriver delegate) {
        Config config = ConfigFactory.systemProperties().withFallback(ConfigFactory.load());
        return create(delegate,config);
    }

    static SelfHealingDriver create(WebDriver delegate, Config config) {
        return create(new SelfHealingEngine(delegate,config));
    }

    static SelfHealingDriver create(SelfHealingEngine engine) {
        ClassLoader classLoader = SelfHealingDriver.class.getClassLoader();
        Class[] interfaces = Stream.concat(
                Arrays.stream(engine.getWebDriver().getClass().getInterfaces()),
                Stream.of(SelfHealingDriver.class))
                .distinct()
                .toArray(Class[]::new);
        Object proxy = Proxy.newProxyInstance(
                classLoader,
                interfaces,
                new SelfHealingProxyInvocationHandler(engine));
        return (SelfHealingDriver) proxy;
    }
}
