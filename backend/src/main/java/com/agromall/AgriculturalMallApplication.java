package com.agromall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgriculturalMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriculturalMallApplication.class, args);
    }
}
