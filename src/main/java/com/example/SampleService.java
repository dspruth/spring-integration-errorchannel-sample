package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
@EnableConfigurationProperties()
public class SampleService {

    public static void main(String[] args) {
        SpringApplication.run(SampleService.class, args);
    }

}
