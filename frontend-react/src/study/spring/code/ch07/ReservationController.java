package com.example.reservation.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    // 1) @Valid만 붙인 경우: 규칙을 어기면 이 메서드는 실행되지 않고 스프링이 400을 응답한다.
    //    응답에는 이유가 없고 서버 로그에만 남는다(실행 결과 참고).
    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(201).body("예약 접수: " + request.guestName());
    }

    // 2) @Valid 바로 뒤에 BindingResult를 받으면, 스프링이 400을 대신 응답하지 않고
    //    검증 결과를 넘겨준다. 어떤 필드가 왜 틀렸는지 직접 응답할 수 있다.
    //    (이 방식은 메서드마다 같은 코드를 반복하게 된다. 8장에서 한곳으로 모은다.)
    @PostMapping("/checked")
    public ResponseEntity<Object> createChecked(@Valid @RequestBody CreateReservationRequest request,
                                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new LinkedHashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }
        return ResponseEntity.status(201).body("예약 접수: " + request.guestName());
    }

    // 3) 주소·쿼리 값도 파라미터에 규칙을 붙여 검사할 수 있다.
    @GetMapping("/{id}")
    public String get(@PathVariable @Positive Long id) {
        return "예약 " + id;
    }
}
