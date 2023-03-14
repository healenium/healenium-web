package com.epam.healenium.model;


import lombok.Data;
import lombok.experimental.Accessors;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Data
@Accessors(chain = true)
public class SessionContext {

    private String sessionId;
    private Map<String, String> enableHealingElements = new HashMap<>();
    private Map<String, String> disableHealingElement = new HashMap<>();
    private Map<String, List<String>> sessionSelectors = new HashMap<>();
    private BiFunction<WebDriver, String, String> functionUrl;

}
