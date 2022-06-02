package org.connectme.core;

import org.connectme.core.authentication.filter.UserAuthenticationFilter;
import org.connectme.core.global.interceptors.LoggingContextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    LoggingContextExtractor loggingDataFilter;

    @Autowired
    UserAuthenticationFilter userAuthenticationFilter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingDataFilter);
    }

    @Bean
    public FilterRegistrationBean<UserAuthenticationFilter> addAuthenticationFilter() {
        FilterRegistrationBean<UserAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(userAuthenticationFilter);
        // add url patterns for authentication
        registration.addUrlPatterns("/interests/*");
        registration.setOrder(2);
        return registration;
    }
}
