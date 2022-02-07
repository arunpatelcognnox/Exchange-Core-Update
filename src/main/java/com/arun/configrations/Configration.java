package com.arun.configrations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class Configration {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	 @Bean
	 public WebMvcConfigurer cors() {
	     return new WebMvcConfigurer() {
	            @Override
	            public void addCorsMappings(CorsRegistry registry) {
	                registry.addMapping("/**")
	                       .allowedOrigins("*")              
	            .allowedMethods("HEAD","GET","POST","PUT","DELETE","PATCH").allowedHeaders("*");       
	       }
	     };
	  }
}
