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

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.firefox.FirefoxDriver.PROFILE;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;

public class SelenideTest {

    private static final int PORT = 8090;

    @Rule
    public TestServer server = new TestServer("SomeNewRoot", PORT);

    @Before
    public void setUp() {
        Configuration.browser = MyGridProvider.class.getName();
    }

    @Test
    public void userCanLoginByUsername() {
        open(String.format("http://localhost:%d", PORT));
        $(By.id("username")).setValue("erin");
        $(By.id("password")).setValue("secret");
        $(By.xpath("//div/button[@type='submit']")).click();
        $(By.id("username")).shouldHave(value("erin"));
    }

    private static class MyGridProvider implements WebDriverProvider {
        @Override
        public WebDriver createDriver(DesiredCapabilities capabilities) {
            FirefoxProfile firefoxProfile = new FirefoxProfile();

            firefoxProfile.setPreference("browser.fullscreen.autohide",true);
            firefoxProfile.setPreference("browser.fullscreen.animateUp",0);

            capabilities.setCapability(PROFILE, firefoxProfile);
            capabilities.setBrowserName("firefox");

            FirefoxOptions options = new FirefoxOptions(capabilities);
            options.setHeadless(true);
            WebDriver wd = new FirefoxDriver(options);
            return SelfHealingDriver.create(wd);
        }
    }

}
