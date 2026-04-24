package com.skinsshowcase.messaging.config;

import com.skinsshowcase.messaging.filter.JwtAuthFilter;
import com.skinsshowcase.messaging.service.JwtSupportService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtSupportService jwtSupportService) {
        var registration = new FilterRegistrationBean<JwtAuthFilter>();
        registration.setFilter(new JwtAuthFilter(jwtSupportService));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
