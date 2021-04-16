package com.example.jay.spring.sock;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public MyListener myListener(AppSettings appSettings) {
        return new MyListener(appSettings);
    }

    @Bean
    @ConfigurationProperties("app-settings")
    public AppSettings appSettings() {
        return new AppSettings();
    }
    
}
