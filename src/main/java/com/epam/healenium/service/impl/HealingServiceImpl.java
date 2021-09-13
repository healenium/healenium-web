/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.service.impl;

import com.epam.healenium.PageAwareBy;
import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.model.LastHealingDataDto;
import com.epam.healenium.service.HealingService;
import com.epam.healenium.treecomparing.Node;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

@Slf4j
public class HealingServiceImpl extends AbstractHealingServiceImpl implements HealingService {

    public HealingServiceImpl(SelfHealingEngine engine) {
        super(engine);
    }

    @Override
    public Optional<WebElement> heal(PageAwareBy pageBy, NoSuchElementException ex) {
        Optional<LastHealingDataDto> lastHealingDataDto = getLastHealingDataDto(pageBy);
        List<Node> paths = lastHealingDataDto.get().getPaths().get(0);
        return healLocator(pageBy, paths, lastHealingDataDto).map(driver::findElement);
    }

}
