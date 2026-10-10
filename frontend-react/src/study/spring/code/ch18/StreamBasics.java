import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 실행: java StreamBasics.java
public class StreamBasics {

    enum Status { CONFIRMED, CANCELED }

    record Reservation(Long id, String guestName, String room, int nights, Status status) {
    }

    // "Stream 시각화" 탭과 같은 예약 6건
    static final List<Reservation> RESERVATIONS = List.of(
            new Reservation(1L, "김민수", "오션뷰", 2, Status.CONFIRMED),
            new Reservation(2L, "박지영", "스탠다드", 1, Status.CANCELED),
            new Reservation(3L, "이수진", "오션뷰", 3, Status.CONFIRMED),
            new Reservation(4L, "최현우", "스위트", 2, Status.CONFIRMED),
            new Reservation(5L, "정다은", "오션뷰", 4, Status.CANCELED),
            new Reservation(6L, "한지민", "스탠다드", 1, Status.CONFIRMED));

    public static void main(String[] args) {
        System.out.println("== 1. 같은 일을 for문과 Stream으로: 확정된 예약의 예약자 이름 ==");
        List<String> byFor = new ArrayList<>();
        for (Reservation r : RESERVATIONS) {
            if (r.status() == Status.CONFIRMED) {
                byFor.add(r.guestName());
            }
        }
        System.out.println("for문   " + byFor);

        List<String> byStream = RESERVATIONS.stream()
                .filter(r -> r.status() == Status.CONFIRMED)   // 조건에 맞는 것만 남긴다
                .map(Reservation::guestName)                     // 다른 값으로 바꾼다
                .toList();                                       // 리스트로 모은다
        System.out.println("Stream  " + byStream);

        System.out.println();
        System.out.println("== 2. 자주 쓰는 연산 ==");
        System.out.println("숙박 긴 순   " + RESERVATIONS.stream()
                .sorted(Comparator.comparing(Reservation::nights).reversed())
                .map(r -> r.guestName() + "(" + r.nights() + "박)")
                .toList());
        System.out.println("객실 종류    " + RESERVATIONS.stream().map(Reservation::room).distinct().toList());
        System.out.println("확정 건수    " + RESERVATIONS.stream().filter(r -> r.status() == Status.CONFIRMED).count());
        System.out.println("확정 숙박 합 " + RESERVATIONS.stream()
                .filter(r -> r.status() == Status.CONFIRMED)
                .mapToInt(Reservation::nights)                   // int로 바꾸면 sum, average를 쓸 수 있다
                .sum());
        System.out.println("4박 이상 있음? " + RESERVATIONS.stream().anyMatch(r -> r.nights() >= 4));
        Optional<Reservation> firstSuite = RESERVATIONS.stream().filter(r -> r.room().equals("스위트")).findFirst();
        System.out.println("첫 스위트    " + firstSuite.map(Reservation::guestName).orElse("없음"));

        System.out.println();
        System.out.println("== 3. 모으기(Collectors) ==");
        Map<String, Long> countByRoom = RESERVATIONS.stream()
                .collect(Collectors.groupingBy(Reservation::room, Collectors.counting()));
        System.out.println("객실별 건수      " + countByRoom);

        Map<String, Integer> nightsByRoom = RESERVATIONS.stream()
                .collect(Collectors.groupingBy(Reservation::room, Collectors.summingInt(Reservation::nights)));
        System.out.println("객실별 숙박 합   " + nightsByRoom);

        Map<Boolean, List<String>> confirmedOrNot = RESERVATIONS.stream()
                .collect(Collectors.partitioningBy(r -> r.status() == Status.CONFIRMED,
                        Collectors.mapping(Reservation::guestName, Collectors.toList())));
        System.out.println("확정/취소로 나누기 " + confirmedOrNot);

        System.out.println("이름 이어 붙이기 " + RESERVATIONS.stream()
                .map(Reservation::guestName)
                .collect(Collectors.joining(", ", "[", "]")));

        System.out.println();
        System.out.println("== 4. 자주 만나는 예외 ==");
        // (1) Stream은 한 번만 쓸 수 있다
        Stream<Reservation> stream = RESERVATIONS.stream();
        stream.count();
        try {
            stream.count();
        } catch (IllegalStateException e) {
            System.out.println("다시 사용  " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // (2) toList()가 돌려준 리스트는 바꿀 수 없다
        List<String> names = RESERVATIONS.stream().map(Reservation::guestName).toList();
        try {
            names.add("홍길동");
        } catch (UnsupportedOperationException e) {
            System.out.println("toList에 추가  " + e.getClass().getSimpleName());
        }

        // (3) toMap은 키가 겹치면 예외. 겹칠 때 어떻게 합칠지 정해 줘야 한다
        try {
            RESERVATIONS.stream().collect(Collectors.toMap(Reservation::room, Reservation::guestName));
        } catch (IllegalStateException e) {
            System.out.println("toMap 키 중복  " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        Map<String, String> guestsByRoom = RESERVATIONS.stream()
                .collect(Collectors.toMap(Reservation::room, Reservation::guestName, (a, b) -> a + ", " + b));
        System.out.println("합치는 방법 지정 " + guestsByRoom);
    }
}
