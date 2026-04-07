package com.cyphershare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CypherShareApplication {
    public static void main(String[] args) {
        SpringApplication.run(CypherShareApplication.class, args);
    }
}