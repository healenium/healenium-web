package com.epam.healenium.service.impl;

import com.epam.healenium.PageAwareBy;
import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.model.HealeniumSelectorImitatorDto;
import com.epam.healenium.model.HealingCandidateDto;
import com.epam.healenium.model.LastHealingDataDto;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.MetricsDto;
import com.epam.healenium.model.RequestDto;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;


@Slf4j
public abstract class AbstractHealingServiceImpl {

    protected final SelfHealingEngine engine;
    protected final WebDriver driver;

    public AbstractHealingServiceImpl(SelfHealingEngine engine) {
        this.engine = engine;
        this.driver = engine.getWebDriver();
    }

    /**
     * Heal Locator method
     *
     * @param pageBy             - source By
     * @param paths              - source path to locator
     * @param lastHealingDataDto - etalon data/successfully test data
     * @return healed locator
     */
    protected Optional<By> healLocator(PageAwareBy pageBy,
                                       List<Node> paths,
                                       Optional<LastHealingDataDto> lastHealingDataDto) {
        String pageContent = pageSource();
        Locator userLocator = engine.getClient().getMapper().byToLocator(pageBy.getBy());
        MetricsDto metricsDto = new MetricsDto()
                .setCurrentDom(pageContent)
                .setUserSelector(userLocator)
                .setPreviousSuccessfulDom(lastHealingDataDto.get().getPageContent());

        List<Scored<By>> choices = engine.findNewLocations(pageContent, paths, metricsDto);

        HealingCandidateDto mainHealingCandidate = metricsDto.getMainHealingCandidate();
        if (mainHealingCandidate != null) {
            imitateMainCandidate(userLocator, mainHealingCandidate, choices);
        }
        Optional<Scored<By>> result = choices.stream().findFirst();
        if (!result.isPresent()) {
            log.warn("New element locators have not been found");
            double scoreCap = engine.getScoreCap();
            log.warn("Score property={} is bigger than healing's locator score", scoreCap);
        } else {
            saveHealedElement(pageBy, pageContent, metricsDto, choices, result);
        }
        return result.map(Scored::getValue);
    }

    /**
     * Save healed locator to db
     *
     * @param pageBy      - source By
     * @param pageContent - current page content
     * @param metricsDto  - list of metrics data
     * @param choices     - all approach healed locators
     * @param result      - target locator
     */
    private void saveHealedElement(PageAwareBy pageBy, String pageContent, MetricsDto metricsDto,
                                   List<Scored<By>> choices, Optional<Scored<By>> result) {
        Scored<By> healed = result.get();
        log.warn("Using healed locator: {}", result);
        byte[] screenshot = captureScreen(healed);
        metricsDto.setHealedSelector(engine.getClient().getMapper().byToLocator(healed.getValue()));
        String healingTime = engine.getHealingTime();

        // build request and send it to server
        RequestDto requestDto = engine.getClient().getMapper().buildDto(pageBy.getBy(), pageContent, choices, healed, screenshot);
        engine.getClient().healRequest(requestDto, screenshot, healingTime, metricsDto);
    }

    protected Optional<LastHealingDataDto> getLastHealingDataDto(PageAwareBy pageBy) {
        return engine.getClient().getLastHealingData(pageBy.getBy());
    }

    /**
     * Call to imitate service and replace main candidate from response value.
     *
     * @param userLocator          - user source locator
     * @param mainHealingCandidate - main target Node
     * @param choices              - rest candidates
     */
    protected void imitateMainCandidate(Locator userLocator, HealingCandidateDto mainHealingCandidate, List<Scored<By>> choices) {
        Node targetNode = mainHealingCandidate.getNode();
        Double score = mainHealingCandidate.getScore();
        if (targetNode != null) {
            HealeniumSelectorImitatorDto healeniumSelectorImitator = new HealeniumSelectorImitatorDto()
                    .setUserSelector(userLocator)
                    .setTargetNode(targetNode);
            List<Locator> imitatedLocators = engine.getClient().imitate(healeniumSelectorImitator);
            engine.toLocator(imitatedLocators, score)
                    .ifPresent(by -> {
                        choices.remove(0);
                        choices.add(0, by);
                    });
        }
    }

    /**
     * Create screenshot of healed element
     *
     * @param byScored - healed locator
     * @return path to screenshot location
     */
    protected byte[] captureScreen(Scored<By> byScored) {
        WebElement element = driver.findElement(byScored.getValue());
        if (engine.isHealingBacklighted()) {
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("arguments[0].style.border='3px solid red'", element);
        }
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    /**
     * Get page content by Driver
     *
     * @return page source
     */
    protected String pageSource() {
        if (driver instanceof JavascriptExecutor) {
            return ((JavascriptExecutor) driver).executeScript("return document.body.outerHTML;").toString();
        } else {
            return driver.getPageSource();
        }
    }
}
