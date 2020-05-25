package com.epam.healenium.service;

import com.epam.healenium.PageAwareBy;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.Optional;

public interface HealingService {

    Optional<WebElement> heal(PageAwareBy pageBy, NoSuchElementException e);

}
