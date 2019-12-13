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

import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class IFrameTest {

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
    public void testIFrame() {
        PageAwareBy locator =
                PageAwareBy.by(IFrameTest.class.getSimpleName(), By.cssSelector("input.framebutton"));
        driver.get("http://localhost:" + PORT);
        WebElement element = driver.switchTo().frame("internal").findElement(locator);
        Assert.assertNotNull(element);
    }

    @After
    public void close() {
        driver.quit();
    }
}
