package com.gamesbar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GamesBarApp {
    public static void  main(String [] args){

        SpringApplication.run(GamesBarApp.class,args);
    }
}


/*cd backend
mvn spring-boot:run */