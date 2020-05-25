/**
 * Healenium-web Copyright (C) 2019 EPAM Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.epam.healenium.tests;

import com.epam.healenium.AbstractBackendIT;
import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.driver.InitDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WaitersTest extends AbstractBackendIT {

    private SelfHealingDriver driver;

    @BeforeEach
    public void createDriver() {
        driver = InitDriver.getDriver();
    }

    @Test
    public void name() {
        driver.get("https://accounts.google.com/signin/v2/identifier?hl=en");
        WebDriverWait waiter = new WebDriverWait(driver, 10);
        waiter.until(ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector("input#Email"))));
        driver.findElement(By.cssSelector("a.need-help")).click();
        waiter.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("input#Email")));
        driver.findElement(By.id("identifier")).sendKeys("email");
        Assertions.assertEquals( "email", driver.findElement(By.id("identifier")).getAttribute("value"));
    }

    @AfterEach
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}
