package com.saif.JobNet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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
