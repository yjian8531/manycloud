package com.core.manycloudadmin.config;

import com.core.manycloudadmin.filter.AdminInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class InterceptorConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private AdminInterceptor adminInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(adminInterceptor).addPathPatterns("/admin/**").excludePathPatterns(
                "/admin/login"
        );

        registry.addInterceptor(adminInterceptor).addPathPatterns("/region/**").excludePathPatterns(
                "/accelerate/get/all"
        );

        registry.addInterceptor(adminInterceptor).addPathPatterns("/node/**");

        registry.addInterceptor(adminInterceptor).addPathPatterns("/user/**");

        registry.addInterceptor(adminInterceptor).addPathPatterns("/instance/**");

        registry.addInterceptor(adminInterceptor).addPathPatterns("/finance/**");


    }
}