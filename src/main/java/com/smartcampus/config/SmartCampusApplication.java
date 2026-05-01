package com.smartcampus.config;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        System.out.println("=== SmartCampusApplication is loading ===");
        packages("com.smartcampus");
        register(JacksonFeature.class);
        System.out.println("=== Packages registered ===");
    }
}