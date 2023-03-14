package com.epam.healenium.function;

import org.openqa.selenium.WebDriver;

import java.util.function.BiFunction;

public class FullUrlFunction implements BiFunction<WebDriver, String, String> {

    @Override
    public String apply(WebDriver webDriver, String url) {
        if (url == null) {
            url = webDriver.getCurrentUrl();
        }
        return url;
    }
}
