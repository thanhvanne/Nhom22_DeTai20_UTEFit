package com.utefit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class FeatureClockConfig {
    @Bean
    Clock featureClock() {
        return Clock.systemUTC();
    }
}
