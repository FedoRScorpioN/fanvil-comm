package ru.utss.fanvilcomm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FanvilCommApplication {

    public static void main(String[] args) {
        SpringApplication.run(FanvilCommApplication.class, args);
    }
}