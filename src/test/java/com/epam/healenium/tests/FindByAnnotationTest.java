/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.tests;

import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.annotation.PageAwareFindBy;
import com.epam.healenium.driver.InitDriver;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.function.Function;

public class FindByAnnotationTest {

    private static final String CUSTOM_PAGE_NAME = "CustomPageName";

    private static SelfHealingDriver driver;

    @BeforeAll
    public static void createDriver() {
        if (driver == null) {
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
    public void testWithDefaultPageName() {
        test(GooglePage::getSearchBox);
    }

    @Test
    public void testWithCustomPageName() {
        test(GooglePage::getCustomSearchBox);
    }

    private void test(Function<GooglePage, WebElement> elementGetter) {
        driver.get("http://www.google.com/");
        GooglePage page = PageFactory.initElements(driver, GooglePage.class);
        WebElement inputElement = elementGetter.apply(page);
        // annotation-driven element is lazy, need to do something with it
        inputElement.sendKeys("search");
        Assertions.assertEquals(inputElement, driver.findElement(By.name("q")));
    }

    public static class GooglePage {

        @PageAwareFindBy(findBy = @FindBy(name = "q"))
        private WebElement searchBox;

        @PageAwareFindBy(page = CUSTOM_PAGE_NAME, findBy = @FindBy(name = "q"))
        private WebElement customSearchBox;

        public GooglePage(WebDriver driver) {
        }

        public WebElement getSearchBox() {
            return searchBox;
        }

        public WebElement getCustomSearchBox() {
            return customSearchBox;
        }
    }
}
