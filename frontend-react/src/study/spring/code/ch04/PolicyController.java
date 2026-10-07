package com.example.reservation;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PolicyController {

    private final ReservationProperties properties;
    private final String appName;
    private final Environment environment;

    public PolicyController(ReservationProperties properties,
                            // 값 하나만 필요하면 @Value로 바로 받는다. ${이름:기본값} 형식.
                            @Value("${spring.application.name:unknown}") String appName,
                            Environment environment) {
        this.properties = properties;
        this.appName = appName;
        this.environment = environment;
    }

    // 지금 어떤 설정값으로 동작하는지 보여 준다. 운영 서버가 의도한 프로필로 떴는지 확인할 때 쓰는 식이다.
    @GetMapping("/policy")
    public Map<String, Object> policy() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("app", appName);
        result.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        result.put("maxNights", properties.maxNights());
        result.put("checkinTime", properties.checkinTime().toString());
        result.put("notice", properties.notice());
        return result;
    }
}
