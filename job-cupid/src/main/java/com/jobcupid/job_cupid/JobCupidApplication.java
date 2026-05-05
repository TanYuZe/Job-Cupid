package com.jobcupid.job_cupid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobCupidApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobCupidApplication.class, args);
	}

}
