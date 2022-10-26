package com.epam.healenium.handlers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public interface SelfHealingHandler {

    WebElement findElement(By by);

    List<WebElement> findElements(By by);

    WebElement wrapElement(WebElement element, ClassLoader loader);

    WebDriver.TargetLocator wrapTarget(WebDriver.TargetLocator locator, ClassLoader loader);
}
