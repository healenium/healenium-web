package com.epam.healenium.function;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;

import java.util.function.BiFunction;

public class EmptyUrlFunction implements BiFunction<WebDriver, String, String> {

    @Override
    public String apply(WebDriver webDriver, String url) {
        return StringUtils.EMPTY;
    }
}
