package io.github.nikilsaini.outreach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ColdEmailerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ColdEmailerApplication.class, args);
	}

}
