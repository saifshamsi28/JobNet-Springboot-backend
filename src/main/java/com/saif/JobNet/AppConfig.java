package com.saif.JobNet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

//    @Bean
//    public WebMvcConfigurer corsConfigurer(){
//        return addCorsMappings(registry) ->{
//            registry.addMapping("/a[")
//        }
//    }
}
