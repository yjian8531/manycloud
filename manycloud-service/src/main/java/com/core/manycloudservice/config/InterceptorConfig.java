package com.core.manycloudservice.config;

import com.core.manycloudservice.filter.UserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class InterceptorConfig extends WebMvcConfigurerAdapter {

    //@Autowired
    //private AdminInterceptor adminInterceptor;

    @Autowired
    private UserInterceptor userInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(userInterceptor).addPathPatterns("/user/**").excludePathPatterns(
                        "/user/verify/img",
                        "/user/verify/login/email",
                        "/user/register",
                        "/user/upwd/img",
                        "/user/verify/upwd/email",
                        "/user/retrieve/pwd",
                        "/user/login"
                );
        registry.addInterceptor(userInterceptor).addPathPatterns("/order/**").excludePathPatterns(
                "/order/query/price",
                "/order/query/periodtype"
        );

        registry.addInterceptor(userInterceptor).addPathPatterns("/pay/**").excludePathPatterns(
                "/pay/wechat/notify",
                "/pay/stripe/notify",
                "/pay/alipay/back"
        );

        registry.addInterceptor(userInterceptor).addPathPatterns("/instance/**");

        registry.addInterceptor(userInterceptor).addPathPatterns("/finance/**").excludePathPatterns(
                "/finance/promotion/count"
        );


    }
}