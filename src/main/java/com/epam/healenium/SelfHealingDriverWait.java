package com.epam.healenium;

import com.epam.healenium.model.SessionContext;
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
        if (!(webDriver instanceof SelfHealingDriver)) {
            return super.until(isTrue);
        }
        SelfHealingDriver hlmDriver = (SelfHealingDriver) webDriver;
        SessionContext context = hlmDriver.getCurrentEngine().getSessionContext();
        // Fix for #314: a wait nested inside another wait must restore the enclosing
        // flag state on exit instead of blindly clearing it. Otherwise the inner
        // wait's exit disables wait-mode while the outer wait is still running.
        boolean prevWaitCommand = context.isWaitCommand();
        boolean prevFindElementWaitCommand = context.isFindElementWaitCommand();
        context.setWaitCommand(true);
        try {
            return super.until(isTrue);
        } catch (TimeoutException timeoutException) {
            if (context.isFindElementWaitCommand()) {
                context.setWaitCommand(false);
                context.setFindElementWaitCommand(false);
                V value = isTrue.apply(webDriver);
                if (value != null && (Boolean.class != value.getClass() || Boolean.TRUE.equals(value))) {
                    return value;
                }
            }
            throw timeoutException;
        } finally {
            context.setWaitCommand(prevWaitCommand);
            context.setFindElementWaitCommand(prevFindElementWaitCommand);
        }
    }
}
