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

import com.epam.healenium.annotation.PageAwareFindBy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.function.Function;

public class FindByAnnotationTest {

    private static final String CUSTOM_PAGE_NAME = "CustomPageName";

    private SelfHealingDriver driver;

    @Before
    public void init() {
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        WebDriver delegate = new ChromeDriver(options);
        driver = SelfHealingDriver.create(delegate);
    }

    @Test
    public void testWithDefaultPageName() {
        test(GooglePage.class.getSimpleName(), GooglePage::getSearchBox);
    }

    @Test
    public void testWithCustomPageName() {
        test(CUSTOM_PAGE_NAME, GooglePage::getCustomSearchBox);
    }

    private void test(String pageName, Function<GooglePage, WebElement> elementGetter) {
        driver.get("http://www.google.com/");
        GooglePage page = PageFactory.initElements(driver, GooglePage.class);
        WebElement inputElement = elementGetter.apply(page);
        // annotation-driven element is lazy, need to do something with it
        inputElement.sendKeys("search");
        PageAwareBy locator = PageAwareBy.by(pageName, By.name("q"));
        By newLocation = driver.getCurrentEngine().findNewLocations(locator, driver.getPageSource()).get(0).getValue();
        Assert.assertEquals(inputElement, driver.findElement(newLocation));
    }

    @After
    public void destroy() {
        if (driver != null) {
            driver.quit();
        }
    }

    public static class GooglePage {

        @PageAwareFindBy(findBy = @FindBy(name = "q"))
        private WebElement searchBox;

        @PageAwareFindBy(page = CUSTOM_PAGE_NAME,
            findBy = @FindBy(name = "q"))
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
