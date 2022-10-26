package com.epam.healenium.mapper.by;


import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import java.util.function.Function;

public class ByIdOrNameMapper implements Function<By, String[]> {

    @Override
    public String[] apply(By by) {
        String[] locatorParts = by.toString().split("\"", 2);
        locatorParts[1] = StringUtils.chop(locatorParts[1]);
        return locatorParts;
    }
}
