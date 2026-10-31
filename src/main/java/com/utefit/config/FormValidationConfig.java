package com.utefit.config;

import com.utefit.validation.FormValidationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FormValidationConfig implements WebMvcConfigurer {
    private final FormValidationInterceptor validation;

    public FormValidationConfig(FormValidationInterceptor validation) {
        this.validation = validation;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(validation);
    }
}
