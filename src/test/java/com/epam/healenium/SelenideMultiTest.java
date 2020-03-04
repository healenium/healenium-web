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
package com.epam.healenium;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverProvider;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class SelenideMultiTest {

    private static final int PORT = 8090;

    @Rule
    public TestServer server = new TestServer("SomeNewRoot", PORT);

    @BeforeClass
    public static void setUp() {
        Configuration.browser = MyGridProvider.class.getName();
    }

    @Before
    public void before() {
        open(String.format("http://localhost:%d", PORT));
        $(By.id("user_name")).click();
    }

    @Test
    public void userNameTest() {
        $(By.id("user_name")).setValue("erin");
        $(By.id("user_name")).shouldHave(value("erin"));
    }

    @Test
    public void passwordTest() {
        $(By.id("password")).setValue("secret");
        $(By.id("password")).shouldHave(value("secret"));
    }

    @After
    public void after() {
        $(By.xpath("//div/button[@type='submit']")).click();
    }

    private static class MyGridProvider implements WebDriverProvider {
        @Override
        public WebDriver createDriver(DesiredCapabilities capabilities) {
            WebDriverManager.chromedriver().setup();
            capabilities.setBrowserName("chrome");
            ChromeOptions options = new ChromeOptions();
            options.setHeadless(true);
            options.merge(capabilities);
            WebDriver wd = new ChromeDriver(options);
            return SelfHealingDriver.create(wd);
        }
    }
}
