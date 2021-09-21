package com.epam.healenium.service;

import com.epam.healenium.PageAwareBy;
import org.openqa.selenium.WebElement;

import java.util.List;

public interface HealingElementsService {

    List<WebElement> heal(PageAwareBy pageBy, List<WebElement> pageElements);
}
