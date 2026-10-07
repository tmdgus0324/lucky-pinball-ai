package com.example.reservation.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

// 인터셉터: 스프링 MVC 단계에서 DispatcherServlet이 컨트롤러를 호출하기 전후에 실행된다.
// 필터와 달리 "어떤 컨트롤러 메서드가 처리할지"(handler)를 알 수 있다.
@Component
public class TimingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TimingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            log.info("[인터셉터] preHandle → {}.{}()", method.getBeanType().getSimpleName(), method.getMethod().getName());
        } else {
            log.info("[인터셉터] preHandle → {}", handler.getClass().getSimpleName());
        }
        return true;   // false를 돌려주면 컨트롤러를 호출하지 않고 여기서 끝난다(9장 로그인 확인)
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        log.info("[인터셉터] postHandle (컨트롤러가 정상 종료됐을 때만)");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        log.info("[인터셉터] afterCompletion (예외: {})", ex == null ? "없음" : ex.getClass().getSimpleName());
    }
}
