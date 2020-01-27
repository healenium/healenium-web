/**
 * Healenium-web Copyright (C) 2019 EPAM Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.epam.healenium;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;

public class ActionsTest {

    private SelfHealingDriver driver;

    @Before
    public void createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        WebDriver delegate = new ChromeDriver(options);
        driver = SelfHealingDriver.create(delegate);
    }

    @Test
    public void name() {
        driver.get("http://www.google.com/");
        try {
            WebElement element = driver.findElement(By.cssSelector("#tsf div.J9leP"));
            // open virtual keyboard
            new Actions(driver)
                .moveToElement(element)
                .click()
                .perform();
            // wait till it appear
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            // click on some keys
            new Actions(driver)
                .click(driver.findElement(By.id("K89")))
                .click(driver.findElement(By.id("K66")))
                .perform();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void userExecuteJS() {
        driver.get("https://google.com");
        String value = ((JavascriptExecutor) driver).executeScript("return document.querySelector('img#hplogo').getAttribute('alt')").toString();
        Assert.assertTrue(value.equalsIgnoreCase("Google"));
    }

    @After
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}
