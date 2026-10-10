import java.util.List;
import java.util.Optional;

// 실행: java OptionalBasics.java
public class OptionalBasics {

    record Reservation(Long id, String guestName, String room, int nights) {
    }

    static final List<Reservation> RESERVATIONS = List.of(
            new Reservation(1L, "김민수", "오션뷰", 2),
            new Reservation(2L, "박지영", "스탠다드", 1));

    // "없을 수도 있다"를 반환 타입으로 알려 준다. JPA의 findById도 Optional을 돌려준다(11장).
    static Optional<Reservation> findById(Long id) {
        return RESERVATIONS.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    static String defaultName() {
        System.out.println("    (defaultName() 실행됨)");
        return "손님";
    }

    public static void main(String[] args) {
        System.out.println("== 1. 값 꺼내기 ==");
        System.out.println("1번 있음?        " + findById(1L).isPresent());
        System.out.println("9번 있음?        " + findById(9L).isPresent());
        System.out.println("1번 이름(map)    " + findById(1L).map(Reservation::guestName).orElse("없음"));
        System.out.println("9번 이름(map)    " + findById(9L).map(Reservation::guestName).orElse("없음"));
        findById(2L).ifPresent(r -> System.out.println("2번이 있으면 실행 " + r.guestName()));

        System.out.println();
        System.out.println("== 2. orElse와 orElseGet의 차이 ==");
        System.out.println("orElse, 값 있음");
        String a = findById(1L).map(Reservation::guestName).orElse(defaultName());
        System.out.println("  결과 " + a);
        System.out.println("orElseGet, 값 있음");
        String b = findById(1L).map(Reservation::guestName).orElseGet(() -> defaultName());
        System.out.println("  결과 " + b);
        System.out.println("orElseGet, 값 없음");
        String c = findById(9L).map(Reservation::guestName).orElseGet(() -> defaultName());
        System.out.println("  결과 " + c);

        System.out.println();
        System.out.println("== 3. 없으면 예외 ==");
        try {
            findById(9L).orElseThrow(() -> new IllegalArgumentException("없는 예약입니다: 9"));
        } catch (IllegalArgumentException e) {
            System.out.println("orElseThrow  " + e.getMessage());
        }
        try {
            findById(9L).get();   // 값이 없을 때 get()을 부르면
        } catch (Exception e) {
            System.out.println("get()        " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        System.out.println();
        System.out.println("== 4. null을 감쌀 때 ==");
        String nullName = null;
        System.out.println("ofNullable(null) " + Optional.ofNullable(nullName));
        try {
            Optional.of(nullName);
        } catch (NullPointerException e) {
            System.out.println("of(null)         NullPointerException");
        }
    }
}
