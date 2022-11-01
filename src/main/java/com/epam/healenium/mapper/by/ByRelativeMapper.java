package com.epam.healenium.mapper.by;

import org.openqa.selenium.By;
import org.openqa.selenium.support.locators.RelativeLocator;

import java.util.function.Function;

public class ByRelativeMapper implements Function<By, String[]> {

    @Override
    public String[] apply(By by) {
        String[] locatorParts = new String[2];
        locatorParts[0] = "Relative By";
        locatorParts[1] = ((RelativeLocator.RelativeBy) by).getRemoteParameters().toString();
        return locatorParts;
    }
}
