package com.gtc.kalbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan("com.gtc.kalbot")
@EnableScheduling
@SpringBootApplication
public class KalBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(KalBotApplication.class, args);
	}
}
