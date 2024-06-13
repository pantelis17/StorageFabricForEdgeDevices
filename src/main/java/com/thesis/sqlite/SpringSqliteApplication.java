package com.thesis.sqlite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringSqliteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringSqliteApplication.class, args);
    }

}
