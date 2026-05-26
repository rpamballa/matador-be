package com.matador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "Matador")
@SpringBootApplication
public class MatadorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatadorApplication.class, args);
    }
}
