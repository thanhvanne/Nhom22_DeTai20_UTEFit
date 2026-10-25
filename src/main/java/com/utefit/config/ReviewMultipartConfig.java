package com.utefit.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;

@Configuration
public class ReviewMultipartConfig {
    @Bean
    public MultipartConfigElement multipartConfigElement(MultipartProperties properties) {
        return properties.createMultipartConfig();
    }
}
