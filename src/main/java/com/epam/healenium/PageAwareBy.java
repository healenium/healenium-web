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

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Objects;
public class PageAwareBy extends By {

    private final String pageName;
    private final By by;

    private PageAwareBy(String pageName, By by) {
        this.pageName = pageName;
        this.by = by;
    }

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

    public String getPageName() {
        return pageName;
    }

    public By getBy() {
        return by;
    }

    @Override
    public List<WebElement> findElements(SearchContext searchContext) {
        return by.findElements(searchContext);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PageAwareBy that = (PageAwareBy) o;
        return Objects.equals(pageName, that.pageName) &&
               Objects.equals(by, that.by);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pageName, by);
    }

    @Override
    public String toString() {
        return String.format("[%s] on page %s", by.toString(), pageName);
    }
}
