package com.epam.healenium.model;

import com.epam.healenium.PageAwareBy;
import com.epam.healenium.treecomparing.Node;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class Context {

    private PageAwareBy pageAwareBy;
    private NoSuchElementException noSuchElementException;
    private LastHealingDataDto lastHealingData;
    private String pageContent;
    private Locator userLocator;
    private String currentUrl;
    private List<WebElement> elements = new ArrayList<>();
    private List<String> elementIds = new ArrayList<>();

    private List<HealingResult> healingResults = new ArrayList<>();
    private String action;

    //ELements
    private Map<WebElement, List<Node>> newElementsToNodes = new HashMap<>();
    private Map<WebElement, List<Node>> existElementsToNodes = new HashMap<>();


}
