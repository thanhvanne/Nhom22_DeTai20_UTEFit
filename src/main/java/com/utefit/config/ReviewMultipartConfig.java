package com.utefit.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReviewMultipartConfig {
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", 20L * 1024 * 1024, 26L * 1024 * 1024, 0);
    }
}
