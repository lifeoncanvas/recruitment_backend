package com.licht.recruitment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LichtBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LichtBackendApplication.class, args);
        System.out.println("==================================================================");
        System.out.println("   LICHT RECRUITMENT SERVICES - BACKEND API STARTED SUCCESSFULLY   ");
        System.out.println("   Live at: http://localhost:8080/api                             ");
        System.out.println("==================================================================");
    }
}
