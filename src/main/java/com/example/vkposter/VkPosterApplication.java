package com.example.vkposter;

import com.example.vkposter.poster.PosterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class VkPosterApplication {

	@Autowired
	private PosterService posterService;

	public static void main(String[] args) {
		SpringApplication.run(VkPosterApplication.class, args);
	}


	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			posterService.run();
		};
	}

}
