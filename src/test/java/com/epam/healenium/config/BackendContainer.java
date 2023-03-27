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
package com.epam.healenium.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

@Slf4j(topic = "healenium")
public class BackendContainer extends DockerComposeContainer<BackendContainer> {

    private static BackendContainer container;

    private BackendContainer(@NonNull File compose) {
        super(compose);
    }

    public static BackendContainer getInstance() {
        if (container == null) {
            container = new BackendContainer(new File("src/test/resources/compose-test.yml"))
                    .withExposedService("healenium_1", 7878);
        }
        return container;
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }

}