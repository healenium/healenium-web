package com.epam.healenium.utils;

import com.epam.healenium.SelfHealingDriver;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.internal.WrapsElement;

@UtilityClass
@SuppressWarnings("unchecked")
public class ProxyFactory {

    public static <T extends WebDriver> SelfHealingDriver createDriverProxy(ClassLoader loader, InvocationHandler handler, Class<T> clazz) {
        Class<?>[] interfaces = Stream.concat(
            Arrays.stream(clazz.getInterfaces()),
            Stream.of(JavascriptExecutor.class, SelfHealingDriver.class)
        ).distinct().toArray(Class[]::new);

        return (SelfHealingDriver) Proxy.newProxyInstance(loader, interfaces, handler);
    }

    public static <T extends WebElement> T createWebElementProxy(ClassLoader loader, InvocationHandler handler) {
        Class<?>[] interfaces = new Class[]{WebElement.class, WrapsElement.class, Locatable.class};
        return (T) Proxy.newProxyInstance(loader, interfaces, handler);
    }

    public static <T extends WebDriver.TargetLocator> T createTargetLocatorProxy(ClassLoader loader, InvocationHandler handler) {
        Class<?>[] interfaces = new Class[]{WebDriver.TargetLocator.class};
        return (T) Proxy.newProxyInstance(loader, interfaces, handler);
    }

}
