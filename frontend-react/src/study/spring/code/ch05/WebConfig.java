package com.example.reservation.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 인터셉터는 빈으로 만드는 것만으로는 동작하지 않는다. 여기서 등록하고 적용할 주소를 정한다.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TimingInterceptor timingInterceptor;

    public WebConfig(TimingInterceptor timingInterceptor) {
        this.timingInterceptor = timingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(timingInterceptor)
                .addPathPatterns("/rooms/**");   // /rooms로 시작하는 주소에만 적용
    }
}
