package com.upiiz.platform_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(com.upiiz.platform_api.storage.ChatStorageProperties.class)
@SpringBootApplication
public class PlatformApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlatformApiApplication.class, args);
	}

}
