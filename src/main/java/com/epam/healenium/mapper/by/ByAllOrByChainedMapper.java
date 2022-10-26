package com.epam.healenium.mapper.by;

import org.openqa.selenium.By;

import java.util.function.Function;

public class ByAllOrByChainedMapper implements Function<By, String[]> {

    @Override
    public String[] apply(By by) {
        String[] locatorParts = new String[2];
        int endType = by.toString().indexOf("(");
        locatorParts[0] = by.toString().substring(0, endType);
        locatorParts[1] = by.toString().substring(endType + 2, by.toString().length() - 2);
        return locatorParts;
    }
}
