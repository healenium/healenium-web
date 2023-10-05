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

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverProvider;
import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.TestServer;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.*;

@Slf4j(topic = "healenium")
public class SelenideTest {

    @RegisterExtension
    static TestServer server = new TestServer("SomeNewRoot");

    @BeforeEach
    public void before() {
        Configuration.browser = MyGridProvider.class.getName();
        open(String.format("http://localhost:%d", server.getPort()));
        $(By.id("user_name")).click();
    }

    @Test
    public void userCanLoginByUsername() {
        $(By.id("user_name")).setValue("erin");
        $(By.id("password")).setValue("secret");
        $(By.xpath("//div/button[@type='submit']")).click();
        $(By.id("user_name")).shouldHave(value("erin"));
        closeWebDriver();
    }


    @Test
    public void userNameTest() {
        $(By.id("user_name")).setValue("erin");
        $(By.id("user_name")).shouldHave(value("erin"));

        $(By.xpath("//div/button[@type='submit']")).click();
    }

    @Test
    public void passwordTest() {
        $(By.id("password")).setValue("secret");
        $(By.id("password")).shouldHave(value("secret"));

        $(By.xpath("//div/button[@type='submit']")).click();
    }

    private static class MyGridProvider implements WebDriverProvider {
        @Override
        public WebDriver createDriver(Capabilities capabilities) {
            Configuration.timeout = 15000;

            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--remote-allow-origins=*");
            options.merge(capabilities);
            WebDriver wd = new ChromeDriver(options);
            return SelfHealingDriver.create(wd);
        }
    }

}
