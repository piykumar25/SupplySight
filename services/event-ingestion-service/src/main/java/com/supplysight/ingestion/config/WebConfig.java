package com.supplysight.ingestion.config;

import com.supplysight.ingestion.interceptor.QuotaEnforcementInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web configuration to register quota enforcement interceptor. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final QuotaEnforcementInterceptor quotaEnforcementInterceptor;

    public WebConfig(QuotaEnforcementInterceptor quotaEnforcementInterceptor) {
        this.quotaEnforcementInterceptor = quotaEnforcementInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(quotaEnforcementInterceptor)
                .addPathPatterns("/api/v1/events/**")
                .excludePathPatterns("/actuator/**", "/health/**");
    }
}
