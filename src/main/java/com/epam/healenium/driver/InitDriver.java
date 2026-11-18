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
package com.epam.healenium.driver;

import com.epam.healenium.SelfHealingDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Init Self-Healing Driver for tests classes
 */
public class InitDriver {

    public static SelfHealingDriver getDriver(){
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--user-data-dir=/tmp/unique-chrome-profile-" + System.currentTimeMillis());
        applyCiOptions(options);
        WebDriver delegate = new ChromeDriver(options);
        return SelfHealingDriver.createTestDriver(delegate);
    }

    /**
     * Adds flags required to run Chrome on a CI runner (no display, restricted sandbox).
     * Applied only when the CI environment variable is set, so local runs are unchanged.
     */
    public static void applyCiOptions(ChromeOptions options) {
        if (System.getenv("CI") != null) {
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
                    "--window-size=1920,1080");
        }
    }
}
