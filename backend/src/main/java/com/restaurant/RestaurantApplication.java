package com.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Savoria Restaurant Order Management backend.
 *
 * @EnableScheduling activates the @Scheduled job in TableSessionService
 * that auto-expires inactive table sessions every 60 seconds.
 */
@SpringBootApplication
@EnableScheduling
public class RestaurantApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestaurantApplication.class, args);
    }
}
