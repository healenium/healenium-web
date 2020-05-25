package com.epam.healenium;

import com.epam.healenium.config.BackendContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractBackendIT {

    @Container
    public static BackendContainer container = BackendContainer.getInstance();

}