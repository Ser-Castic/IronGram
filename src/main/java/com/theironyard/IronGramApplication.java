package com.theironyard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.concurrent.*;


@SpringBootApplication
@EnableScheduling
public class IronGramApplication {

	public static void main(String[] args) {
		SpringApplication.run(IronGramApplication.class, args);
	}
}
