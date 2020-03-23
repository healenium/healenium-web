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
package com.epam.healenium.annotation;

import com.epam.healenium.PageAwareBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.AbstractFindByBuilder;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactoryFinder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@PageFactoryFinder(PageAwareFindBy.FindByBuilder.class)
public @interface PageAwareFindBy {

    String DEFAULT_PAGE_NAME = "DEFAULT_PAGE";

    /**
     * @return a plain element locator
     */
    FindBy findBy();

    /**
     * Identifies the page name this locator belongs to. By default, uses the class name in which the locator is
     * defined.
     *
     * @return an identifier of the current UI state the element is located on
     */
    String page() default DEFAULT_PAGE_NAME;

    class FindByBuilder extends AbstractFindByBuilder {

        public By buildIt(Object annotation, Field field) {
            PageAwareFindBy pageAwareFindBy = (PageAwareFindBy) annotation;
            FindBy findBy = pageAwareFindBy.findBy();
            assertValidFindBy(findBy);

            By ans = buildByFromShortFindBy(findBy);
            if (ans == null) {
                ans = buildByFromLongFindBy(findBy);
            }

            String actualPageName = pageAwareFindBy.page();
            if (DEFAULT_PAGE_NAME.equals(actualPageName)) {
                actualPageName = field.getDeclaringClass().getSimpleName();
            }
            return PageAwareBy.by(actualPageName, ans);
        }
    }
}
