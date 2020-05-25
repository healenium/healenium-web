package com.epam.healenium.driver;

import com.epam.healenium.SelfHealingDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class InitDriver {

    public static SelfHealingDriver getDriver(){
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        WebDriver delegate = new ChromeDriver(options);
        return SelfHealingDriver.create(delegate);
    }
}
