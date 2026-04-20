package com.example.stu_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RoleBasedAccessInterceptor roleBasedAccessInterceptor;

    public WebMvcConfig(RoleBasedAccessInterceptor roleBasedAccessInterceptor) {
        this.roleBasedAccessInterceptor = roleBasedAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleBasedAccessInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/common/**", "/api/email/**");
    }
}
