package com.epam.healenium.mapper.by;

import org.openqa.selenium.By;

import java.util.function.Function;

public class ByDefaultMapper implements Function<By, String[]> {

    @Override
    public String[] apply(By by) {
        return by.toString().split(":", 2);
    }
}
