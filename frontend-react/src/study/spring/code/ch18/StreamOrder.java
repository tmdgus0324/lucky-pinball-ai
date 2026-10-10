import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 실행: java StreamOrder.java
// Stream이 요소를 어떤 순서로 처리하는지 찍어 본다. "Stream 시각화" 탭이 보여 주는 순서와 같다.
// peek은 지나가는 요소를 바꾸지 않고 들여다보는 연산이라 이런 확인용으로 쓴다.
public class StreamOrder {

    enum Status { CONFIRMED, CANCELED }

    record Reservation(Long id, String guestName, String room, int nights, Status status) {
        String label() {
            return id + "번 " + guestName;
        }
    }

    static final List<Reservation> RESERVATIONS = List.of(
            new Reservation(1L, "김민수", "오션뷰", 2, Status.CONFIRMED),
            new Reservation(2L, "박지영", "스탠다드", 1, Status.CANCELED),
            new Reservation(3L, "이수진", "오션뷰", 3, Status.CONFIRMED),
            new Reservation(4L, "최현우", "스위트", 2, Status.CONFIRMED),
            new Reservation(5L, "정다은", "오션뷰", 4, Status.CANCELED),
            new Reservation(6L, "한지민", "스탠다드", 1, Status.CONFIRMED));

    public static void main(String[] args) {
        System.out.println("== A. filter → map → toList ==");
        List<String> a = RESERVATIONS.stream()
                .peek(r -> System.out.println(r.label() + " 꺼냄"))
                .filter(r -> {
                    boolean ok = r.status() == Status.CONFIRMED;
                    System.out.println("  filter 확정? " + (ok ? "통과" : "걸러짐"));
                    return ok;
                })
                .map(r -> {
                    System.out.println("  map → " + r.guestName());
                    return r.guestName();
                })
                .toList();
        System.out.println("결과 " + a);

        System.out.println();
        System.out.println("== B. filter → map → limit(2) → toList ==");
        List<String> b = RESERVATIONS.stream()
                .peek(r -> System.out.println(r.label() + " 꺼냄"))
                .filter(r -> {
                    boolean ok = r.nights() >= 2;
                    System.out.println("  filter 2박 이상? " + (ok ? "통과" : "걸러짐"));
                    return ok;
                })
                .map(r -> {
                    System.out.println("  map → " + r.guestName());
                    return r.guestName();
                })
                .limit(2)
                .toList();
        System.out.println("결과 " + b);

        System.out.println();
        System.out.println("== C. sorted → map → toList ==");
        List<String> c = RESERVATIONS.stream()
                .peek(r -> System.out.println(r.label() + " 꺼냄 → sorted가 모아 둠"))
                .sorted(Comparator.comparing(Reservation::nights))
                .map(r -> {
                    System.out.println("  map → " + r.guestName() + "(" + r.nights() + "박)");
                    return r.guestName();
                })
                .toList();
        System.out.println("결과 " + c);

        System.out.println();
        System.out.println("== D. groupingBy(객실, counting) ==");
        Map<String, Long> d = RESERVATIONS.stream()
                .peek(r -> System.out.println(r.label() + " 꺼냄 → " + r.room() + " 칸 +1"))
                .collect(Collectors.groupingBy(Reservation::room, Collectors.counting()));
        System.out.println("결과 " + d);
    }
}
