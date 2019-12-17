/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium;

import com.epam.healenium.data.LocatorInfo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.typesafe.config.Config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.remote.Augmenter;

@Log4j2
class SelfHealingProxyInvocationHandler implements InvocationHandler {

    private final LoadingCache<PageAwareBy, WebElement> stash;

    private final WebDriver delegate;
    private final SelfHealingEngine engine;
    private final LocatorInfo info = new LocatorInfo();
    private final Config config;

    SelfHealingProxyInvocationHandler(SelfHealingEngine engine) {
        this.delegate = engine.getWebDriver();
        this.config = engine.getConfig();
        this.engine = engine;

        this.stash = CacheBuilder.newBuilder()
            .maximumSize(300)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(
                new CacheLoader<PageAwareBy, WebElement>() {
                    @Override
                    public WebElement load(PageAwareBy key) {
                        return lookUp(key);
                    }
                });
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "findElement":
                log.debug("Caught findElement: invoking the healing version...");
                return findElement((By) args[0]);
            case "getCurrentEngine":
                log.debug("Caught getCurrentEngine");
                return engine;
            case "getDelegate":
                log.debug("Caught getDelegate");
                return delegate;
            case "switchTo":
                log.debug("Caught switchTo");
                WebDriver.TargetLocator switched = (WebDriver.TargetLocator) method.invoke(delegate, args);
                ClassLoader classLoader = delegate.getClass().getClassLoader();
                return Proxy.newProxyInstance(
                    classLoader,
                    new Class[]{WebDriver.TargetLocator.class},
                    new TargetLocatorProxyInvocationHandler(switched, config, engine));
            default:
                return method.invoke(delegate, args);
        }
    }

    private WebElement findElement(By by) {
        try {
            PageAwareBy pageBy = awareBy(by);
            By inner = pageBy.getBy();
            if (!config.getBoolean("heal-enabled")) {
                return delegate.findElement(inner);
            }
            return stash.get(pageBy);
        } catch (NoSuchElementException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to find element", ex);
            return null;
        }
    }

    /**
     * Search target element on a page
     * @param key will be used for checking|saving in cache
     * @return
     */
    private WebElement lookUp(PageAwareBy key) {
        try {
            WebElement element = delegate.findElement(key.getBy());
            engine.savePath(key, element);
            return element;
        } catch (NoSuchElementException e) {
            log.warn("Failed to find an element using locator {}\nReason: {}\nTrying to heal...", key.getBy().toString(), e.getMessage());
            return heal(key, e).orElse(null);
        }
    }

    private Optional<WebElement> heal(PageAwareBy pageBy, NoSuchElementException e) {
        LocatorInfo.Entry entry = reportBasicInfo(pageBy, e);
        return healLocator(pageBy).map(healed -> {
            reportFailedInfo(pageBy, entry, healed);
            engine.saveLocator(info);
            return delegate.findElement(healed);
        });
    }

    private void reportFailedInfo(PageAwareBy by, LocatorInfo.Entry infoEntry, By healed) {
        String failedByValue = by.getBy().toString();
        int splitIndex = failedByValue.indexOf(':');
        infoEntry.setFailedLocatorType(failedByValue.substring(0, splitIndex).trim());
        infoEntry.setFailedLocatorValue(failedByValue.substring(++splitIndex).trim());

        String healedByValue = healed.toString();
        splitIndex = healedByValue.indexOf(':');
        infoEntry.setHealedLocatorType(healedByValue.substring(0, splitIndex).trim());
        infoEntry.setHealedLocatorValue(healedByValue.substring(++splitIndex).trim());

        infoEntry.setScreenShotPath(captureScreen(healed));
        int pos = info.getElementsInfo().indexOf(infoEntry);
        if (pos != -1) {
            info.getElementsInfo().set(pos, infoEntry);
        } else {
            info.getElementsInfo().add(infoEntry);
        }
    }

    private LocatorInfo.Entry reportBasicInfo(PageAwareBy pageBy, NoSuchElementException e) {
        Optional<StackTraceElement> elOpt = getStackTraceForPageObject(e.getStackTrace(), pageBy.getPageName());
        return elOpt.map(el -> {
            LocatorInfo.PageAsClassEntry entry = new LocatorInfo.PageAsClassEntry();
            entry.setFileName(el.getFileName());
            entry.setLineNumber(el.getLineNumber());
            entry.setMethodName(el.getMethodName());
            entry.setDeclaringClass(el.getClassName());
            return (LocatorInfo.Entry) entry;
        }).orElseGet(() -> {
            LocatorInfo.SimplePageEntry entry = new LocatorInfo.SimplePageEntry();
            entry.setPageName(pageBy.getPageName());
            return entry;
        });
    }

    private Optional<StackTraceElement> getStackTraceForPageObject(StackTraceElement[] elements, String pageName) {
        return Arrays
            .stream(elements)
            .filter(element -> {
                String className = element.getClassName();
                String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
                log.debug("Input: {}, simple: {}", className, simpleClassName);
                return simpleClassName.equals(pageName);
            })
            .findFirst();
    }

    private Optional<By> healLocator(PageAwareBy pageBy) {
        log.debug("* healLocator start: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        List<By> choices = engine.findNewLocations(pageBy, pageSource());
        Optional<By> result = choices.stream().findFirst();
        result.ifPresent(primary ->
            log.warn("Using healed locator: {}", primary.toString()));
        choices.stream().skip(1).forEach(otherChoice ->
            log.warn("Other choice: {}", otherChoice.toString()));
        if (!result.isPresent()) {
            log.warn("New element locators have not been found");
        }
        log.debug("* healLocator finish: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        return result;
    }

    private String captureScreen(By by) {
        WebElement element = findElement(by);
        String path;
        try {
            JavascriptExecutor jse = (JavascriptExecutor) delegate;
            jse.executeScript("arguments[0].style.border='3px solid red'", element);
            WebDriver augmentedDriver = new Augmenter().augment(delegate);
            byte[] source = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BYTES);
            FileHandler.createDir(new File(config.getString("screenshotPath")));
            File file =
                new File(config.getString("screenshotPath") + "screenshot_" + LocalDateTime
                    .now()
                    .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy-hh-mm-ss").withLocale(Locale.US)) + ".png");
            Files.write(file.toPath(), source, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            path = file.getPath().replaceAll("\\\\", "/");
            path = ".." + path.substring(path.indexOf("/sc"));

        } catch (IOException e) {
            path = "Failed to capture screenshot: " + e.getMessage();
        }
        return path;
    }

    private String pageSource() {
        if (delegate instanceof JavascriptExecutor) {
            return ((JavascriptExecutor) delegate).executeScript("return document.body.outerHTML;").toString();
        } else {
            return delegate.getPageSource();
        }
    }

    private PageAwareBy awareBy(By by) {
        return (by instanceof PageAwareBy) ? (PageAwareBy) by : PageAwareBy.by(delegate.getTitle(), by);
    }
}
