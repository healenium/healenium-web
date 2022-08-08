/**
 * Healenium-web Copyright (C) 2019 EPAM Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.epam.healenium.tests;

import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.driver.InitDriver;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;

public class ActionsTest {

    private String urlShaTest = "https://elenastepuro.github.io/test_env/index.html";

    protected static SelfHealingDriver driver;

    @BeforeAll
    public static void createDriver() {
        if (driver == null){
            driver = InitDriver.getDriver();
        }
    }

    @AfterAll
    public static void close() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testFindElementByName() {
        driver.get(urlShaTest);
        try{
            new Actions(driver)
                .moveToElement(driver.findElement(By.name("change_name")))
                .click()
                .sendKeys("search")
                .build()
                .perform();
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void nameFindElementsByName() {
        driver.get(urlShaTest);
        try{
            new Actions(driver)
                .moveToElement(driver.findElements(By.name("change_name")).get(0))
                .click()
                .sendKeys("search")
                .build()
                .perform();
        } catch (Exception e) {
            Assertions.fail();
        }
    }

//    @Ignore
//    @Test
//    public void userExecuteJS() {
//        driver.get(urlShaTest);
//        String value = ((JavascriptExecutor) driver)
//                .executeScript("return document.querySelector('#logo').getAttribute('alt')").toString();
//        Assertions.assertTrue(value.equalsIgnoreCase("Healenium"));
//    }

}
