package com.example.reservation.reservation;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 인터페이스만 만들면 MyBatis가 구현체를 만들어 빈으로 등록한다.
// 메서드 이름이 같은 이름의 XML(ReservationMapper.xml)의 id와 연결된다.
@Mapper
public interface ReservationMapper {

    Reservation findById(Long id);

    List<Reservation> search(ReservationSearch condition);

    int insert(Reservation reservation);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // ${} 위험을 보여 주기 위한 메서드. 실제로는 절대 이렇게 쓰지 않는다.
    List<Reservation> findByGuestNameUnsafe(@Param("guestName") String guestName);
}
