package com.epam.healenium.model;

import com.epam.healenium.treecomparing.Scored;
import lombok.Data;
import lombok.experimental.Accessors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@Data
@Accessors(chain = true)
public class HealedElement {

    private WebElement element;
    private Scored<By> scored;

}
