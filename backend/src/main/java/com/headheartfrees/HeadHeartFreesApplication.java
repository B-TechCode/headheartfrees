package com.headheartfrees;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HeadHeartFreesApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeadHeartFreesApplication.class, args);
    }
}
