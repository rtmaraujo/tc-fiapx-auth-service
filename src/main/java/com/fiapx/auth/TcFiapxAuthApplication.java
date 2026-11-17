package com.fiapx.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TcFiapxAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcFiapxAuthApplication.class, args);
    }
}
