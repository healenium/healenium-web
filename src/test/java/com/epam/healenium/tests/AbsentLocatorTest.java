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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

public class AbsentLocatorTest extends AbstractBackendIT {

    private static final String PAGE_NAME = AbsentLocatorTest.class.getSimpleName();

    private SelfHealingDriver driver;

    @BeforeEach
    public void createDriver() {
        driver = InitDriver.getDriver();
    }

    @Test
    public void name() {
        driver.get("https://google.com/");
        PageAwareBy by = PageAwareBy.by(PAGE_NAME, By.tagName("nonexistenttag"));
        Assertions.assertThrows(NoSuchElementException.class, () -> driver.findElement(by));
    }

    @AfterEach
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}
