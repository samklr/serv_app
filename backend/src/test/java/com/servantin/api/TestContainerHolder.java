package com.servantin.api;

import org.testcontainers.containers.PostgreSQLContainer;

public class TestContainerHolder {
    public static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine");
        postgres.start();
    }
}
