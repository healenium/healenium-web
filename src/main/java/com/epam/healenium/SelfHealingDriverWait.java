package com.epam.healenium;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Function;

public class SelfHealingDriverWait extends WebDriverWait {

    private final WebDriver webDriver;

    public SelfHealingDriverWait(WebDriver driver, Duration timeout) {
        super(driver, timeout);
        this.webDriver = driver;
    }

    public SelfHealingDriverWait(WebDriver driver, Duration timeout, Duration sleep) {
        super(driver, timeout, sleep);
        this.webDriver = driver;
    }

    public SelfHealingDriverWait(WebDriver driver, Duration timeout, Duration sleep, Clock clock, Sleeper sleeper) {
        super(driver, timeout, sleep, clock, sleeper);
        this.webDriver = driver;
    }

    @Override
    public <V> V until(Function<? super WebDriver, V> isTrue) {
        if (webDriver instanceof SelfHealingDriver) {
            SelfHealingDriver hlmDriver = (SelfHealingDriver) webDriver;
            try {
                hlmDriver.getCurrentEngine().getSessionContext().setWaitCommand(true);
                V until = super.until(isTrue);
                hlmDriver.getCurrentEngine().getSessionContext().setWaitCommand(false);
                return until;
            } catch (TimeoutException timeoutException) {
                if (hlmDriver.getCurrentEngine().getSessionContext().isFindElementWaitCommand()) {
                    hlmDriver.getCurrentEngine().getSessionContext().setWaitCommand(false);
                    hlmDriver.getCurrentEngine().getSessionContext().setFindElementWaitCommand(false);
                    V value = isTrue.apply(webDriver);
                    if (value != null && (Boolean.class != value.getClass() || Boolean.TRUE.equals(value))) {
                        return value;
                    }
                }
                throw timeoutException;
            } catch (Exception ex) {
                hlmDriver.getCurrentEngine().getSessionContext().setWaitCommand(false);
                throw ex;
            }
        } else {
            return super.until(isTrue);
        }
    }
}



