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
package com.epam.healenium.tests;

import com.epam.healenium.AbstractBackendIT;
import com.epam.healenium.PageAwareBy;
import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingEngine;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

@TestMethodOrder(Alphanumeric.class)
public class SelfHealingEngineTest extends AbstractBackendIT {

    private static final String PAGE_NAME = SelfHealingEngineTest.class.getSimpleName();

    private SelfHealingDriver driver;

    @BeforeEach
    public void createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        WebDriver delegate = new ChromeDriver(options);
        Config config = ConfigFactory.parseResources("test.conf")
                .withValue("heal-enabled", ConfigValueFactory.fromAnyRef(true)).resolve();
        SelfHealingEngine engine = new SelfHealingEngine(delegate, config);
        driver = SelfHealingDriver.create(engine);
    }

    @Test
    public void findNewLocatorTest() {
        driver.get("https://google.com/");
        PageAwareBy by = PageAwareBy.by(PAGE_NAME, By.xpath("//input[@name='source']"));
        WebElement input = driver.findElement(by);
        By newLocation = driver.getCurrentEngine().findNewLocations(by, driver.getPageSource()).get(0).getValue();
        Assertions.assertEquals(input, driver.findElement(newLocation));
    }

    @Test
    public void locatorHealingTest() {
        final By inputFieldLocator = By.xpath("//input[@name='source']");
        driver.get("https://google.com/");
        PageAwareBy by = PageAwareBy.by(PAGE_NAME, inputFieldLocator);
        WebElement input = driver.findElement(by);
        JavascriptExecutor js = driver.getDelegate();
        js.executeScript("arguments[0].setAttribute('name', 'source_new')", input);
        by = PageAwareBy.by(PAGE_NAME, inputFieldLocator);
        input = driver.findElement(by);
        By newLocation = driver.getCurrentEngine().findNewLocations(by, driver.getPageSource()).get(0).getValue();
        Assertions.assertEquals(input, driver.findElement(newLocation));
    }

    @AfterEach
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}
