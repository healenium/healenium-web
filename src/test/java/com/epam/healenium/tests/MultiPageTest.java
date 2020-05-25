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
import com.epam.healenium.driver.InitDriver;
import com.epam.healenium.treecomparing.Scored;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultiPageTest extends AbstractBackendIT {

    public static final String PAGE_NAME = MultiPageTest.class.getSimpleName();

    private SelfHealingDriver create() {
        return InitDriver.getDriver();
    }

    @Test
    public void name() throws InterruptedException {
        int nThreads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            executor.submit(this::run);
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    private void run() {
        SelfHealingDriver driver = create();
        driver.get("https://duckduckgo.com/");
        PageAwareBy by = PageAwareBy.by(PAGE_NAME, By.cssSelector("form input[type=text]"));
        WebElement input = driver.findElement(by);
        Scored<By> newLocation = driver.getCurrentEngine().findNewLocations(by, driver.getPageSource()).get(0);
        Assertions.assertEquals(input, driver.findElement(newLocation.getValue()));
        driver.quit();
    }
}
