package com.ojtsu26.elearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ElearningApplication {

    public static void main(String[] args) {
        configureDebugFlag();
        SpringApplication.run(ElearningApplication.class, args);
    }

    private static void configureDebugFlag() {
        if (System.getProperty("debug") != null) {
            return;
        }

        System.setProperty("debug", Boolean.toString("true".equalsIgnoreCase(System.getenv("APP_DEBUG"))));
    }
}
