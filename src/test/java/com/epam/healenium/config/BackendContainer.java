package com.epam.healenium.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

@Slf4j
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