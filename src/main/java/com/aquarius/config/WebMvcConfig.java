package com.aquarius.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FiscalContextInterceptor fiscalContextInterceptor;
    private final ActivityLoggingInterceptor activityLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(fiscalContextInterceptor)
                .addPathPatterns("/**");
        // Log di sistema (attività utenti + tempi richieste/risposte), esclusi gli statici
        registry.addInterceptor(activityLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/webjars/**",
                                     "/favicon.ico", "/actuator/**", "/error");
    }
}
