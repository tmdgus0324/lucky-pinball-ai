package com.luckypinball.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 필터가 요청을 직접 끊고 응답을 쓸 때(AdminAuthFilter의 401, RateLimitFilter의 429)도
     * 같은 허용 목록을 써야 한다 — 그 응답들은 DispatcherServlet까지 안 가서, 아래
     * addCorsMappings 설정이 적용 안 되고 CORS 헤더가 빠진 채로 나간다(devhelp/08(구 34) 참고).
     */
    public static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5500", "http://127.0.0.1:5500",
            "http://localhost:5501", "http://127.0.0.1:5501",
            "http://localhost:5173", "http://127.0.0.1:5173",
            "https://lucky-pinball-ai.vercel.app");

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(ALLOWED_ORIGINS.toArray(new String[0]))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
