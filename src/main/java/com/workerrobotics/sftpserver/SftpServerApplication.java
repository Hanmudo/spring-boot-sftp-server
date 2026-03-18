package com.workerrobotics.sftpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SftpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SftpServerApplication.class, args);
    }
}