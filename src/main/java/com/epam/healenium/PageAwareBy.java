/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PageAwareBy extends By {

    @Getter private final String pageName;
    @Getter private final By by;

    /**
     * Instantiates a page-aware locator.
     *
     * @param pageName an identifier of the current UI state the element is located on
     * @param by       the original element locator
     * @return the locator that indicates that the element is a subject to healing.
     */
    public static PageAwareBy by(String pageName, By by) {
        return new PageAwareBy(pageName, by);
    }

    @Override
    public List<WebElement> findElements(SearchContext searchContext) {
        return by.findElements(searchContext);
    }

    @Override
    public String toString() {
        return String.format("[%s] on page %s", by.toString(), pageName);
    }
}
