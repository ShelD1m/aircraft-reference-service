package ru.demo.nsi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class NsiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NsiApplication.class, args);
    }
}
