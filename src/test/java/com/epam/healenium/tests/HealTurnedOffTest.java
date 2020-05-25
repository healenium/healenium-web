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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class HealTurnedOffTest extends AbstractBackendIT {

    private static final String PAGE_NAME = HealTurnedOffTest.class.getSimpleName();

    private SelfHealingDriver driver;

    @BeforeEach
    public void createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        WebDriver delegate = new ChromeDriver(options);
        Config config = ConfigFactory.load("test.conf");
        SelfHealingEngine engine = new SelfHealingEngine(delegate, config);
        driver = SelfHealingDriver.create(engine);
    }

    @Test
    public void exceptionThrownTest() {
        driver.get("https://google.com/");
        PageAwareBy by = PageAwareBy.by(PAGE_NAME, By.tagName("nonexistenttag"));
        Assertions.assertThrows(NoSuchElementException.class, () -> driver.findElement(by));
    }

    @Test
    public void legitLocatorWorkingTest() {
        driver.get("https://google.com/");
        PageAwareBy by = PageAwareBy.by(PAGE_NAME, By.name("q"));
        WebElement input = driver.findElement(by);
        Assertions.assertTrue(input.isDisplayed());
    }

    @AfterEach
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}
