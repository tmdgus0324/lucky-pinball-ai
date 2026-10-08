package com.example.reservation.payment;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

// 결제 서버를 부르는 클라이언트. 외부 API 호출은 이렇게 클래스 하나에 모아 둔다.
public class PaymentClient {

    public record PayRequest(Long reservationId, int amount, String card) {
    }

    public record PayResponse(String approvalNo) {
    }

    private final RestClient restClient;

    public PaymentClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        // 타임아웃을 꼭 정한다. 정하지 않으면 상대 서버가 응답하지 않을 때 끝없이 기다릴 수 있다.
        // JDK 11의 HttpClient를 쓰는 구현. 연결 시간은 HttpClient에, 응답 대기 시간은 factory에 정한다.
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public PayResponse pay(PayRequest request) {
        try {
            return restClient.post()
                    .uri("/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    // 4xx: 요청이 거절됨(카드 한도 초과 등). 다시 시도해도 같은 결과
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new PaymentException(PaymentException.Reason.REJECTED,
                                "결제 거절(" + res.getStatusCode().value() + ")");
                    })
                    // 5xx: 결제 서버 쪽 문제. 잠시 뒤 다시 시도하면 될 수도 있다
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new PaymentException(PaymentException.Reason.SERVER_ERROR,
                                "결제 서버 오류(" + res.getStatusCode().value() + ")");
                    })
                    .body(PayResponse.class);
        } catch (ResourceAccessException e) {
            // 연결 실패, 응답 시간 초과 같은 네트워크 문제는 이 예외로 온다. 원인(getCause)으로 구분한다.
            // 단, 어떤 예외로 감싸져 오는지는 HTTP 클라이언트 구현마다 다르다(정리 5절). 직접 실행해서 확인한다.
            throw new PaymentException(PaymentException.Reason.UNAVAILABLE,
                    "결제 서버 연결 실패(" + e.getCause().getClass().getSimpleName() + ")");
        }
    }
}
