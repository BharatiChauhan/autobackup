package com.backup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableScheduling
@ComponentScan("com.backup.*")
//@EnableAutoConfiguration
public class AutobackupApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutobackupApplication.class, args);
	}



}
