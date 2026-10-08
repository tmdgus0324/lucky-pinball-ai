package com.example.reservation.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 네 가지 경우 모두 "예약을 저장한 뒤 결제가 실패한다". 예약이 남는지(커밋) 사라지는지(롤백)를 본다.
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    // 1) unchecked 예외(RuntimeException) → 롤백. 예약이 남지 않는다.
    @Transactional
    public void reserveWithRuntimeFailure(String guestName) {
        repository.save(new Reservation(guestName));
        throw new IllegalStateException("결제 서버 응답 없음");
    }

    // 2) checked 예외 → 기본 설정에서는 롤백하지 않고 커밋한다. 결제는 실패했는데 예약이 남는다.
    @Transactional
    public void reserveWithCheckedFailure(String guestName) throws PaymentException {
        repository.save(new Reservation(guestName));
        throw new PaymentException("카드 한도 초과");
    }

    // 3) rollbackFor를 주면 checked 예외도 롤백한다.
    @Transactional(rollbackFor = Exception.class)
    public void reserveWithCheckedFailureRollback(String guestName) throws PaymentException {
        repository.save(new Reservation(guestName));
        throw new PaymentException("카드 한도 초과");
    }

    // 4) 같은 클래스 안에서 호출: @Transactional이 붙은 메서드를 this로 부르면 트랜잭션이 시작되지 않는다.
    public void reserveFromInside(String guestName) {
        saveAndFail(guestName);   // this.saveAndFail(...) 과 같다
    }

    @Transactional
    public void saveAndFail(String guestName) {
        log.info("saveAndFail 안에서 트랜잭션 진행 중인가? {}",
                TransactionSynchronizationManager.isActualTransactionActive());
        repository.save(new Reservation(guestName));
        throw new IllegalStateException("결제 서버 응답 없음");
    }

    public long count() {
        return repository.count();
    }
}
