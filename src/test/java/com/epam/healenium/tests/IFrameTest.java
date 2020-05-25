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
import com.epam.healenium.TestServer;
import com.epam.healenium.driver.InitDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class IFrameTest extends AbstractBackendIT {

    @RegisterExtension
    public static final TestServer server = new TestServer(IFrameTest.class.getSimpleName());

    private SelfHealingDriver driver;

    @BeforeEach
    public void createDriver() {
        driver = InitDriver.getDriver();
    }

    @Test
    public void testIFrame() {
        PageAwareBy locator = PageAwareBy.by(IFrameTest.class.getSimpleName(), By.cssSelector("input.framebutton"));
        driver.get("http://localhost:" + server.getPort());
        WebElement element = driver.switchTo().frame("internal").findElement(locator);
        Assertions.assertNotNull(element);
    }

    @AfterEach
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}
