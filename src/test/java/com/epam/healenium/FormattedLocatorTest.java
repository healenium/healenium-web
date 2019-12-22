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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class FormattedLocatorTest {

    private static final String PAGE_NAME = SelfHealingEngineTest.class.getSimpleName();
    private static final int PORT = 8090;

    @Rule
    public TestServer server = new TestServer(getClass().getSimpleName(), PORT);

    private SelfHealingDriver driver;

    @Before
    public void createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        WebDriver delegate = new ChromeDriver(options);
        driver = SelfHealingDriver.create(delegate);
    }

    @Test
    public void testNotThrowingExceptionWithFormattedLocator() {
        driver.get(String.format("http://localhost:%d", PORT));
        selectItem("inner");
        selectItem("inner2");
    }

    @After
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void selectItem(String itemName) {
        PageAwareBy by = PageAwareBy.by(PAGE_NAME, By.xpath(String.format("//div[@title='%s']", itemName)));
        driver.findElement(by).click();
    }
}
