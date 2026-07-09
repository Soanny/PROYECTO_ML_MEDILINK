package com.medicas.consultas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ConsultasApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsultasApplication.class, args);
	}

}
