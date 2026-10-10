import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

// 실행: java LambdaBasics.java
// (Java 11부터 파일 하나짜리 프로그램은 javac 없이 바로 실행할 수 있다)
public class LambdaBasics {

    record Reservation(Long id, String guestName, String room, int nights) {
    }

    // 직접 만든 함수형 인터페이스. 추상 메서드가 딱 하나여야 람다로 쓸 수 있다.
    @FunctionalInterface
    interface PriceRule {
        int apply(int price);
    }

    public static void main(String[] args) {
        Reservation kim = new Reservation(1L, "김민수", "오션뷰", 2);
        Reservation park = new Reservation(2L, "박지영", "스탠다드", 1);
        Reservation lee = new Reservation(3L, "이수진", "오션뷰", 3);
        List<Reservation> reservations = new ArrayList<>(List.of(kim, park, lee));

        System.out.println("== 1. 익명 클래스 → 람다 → 메서드 참조 ==");
        // Java 7까지: 이름 없는 클래스를 그 자리에서 만들어 넘긴다
        reservations.sort(new Comparator<Reservation>() {
            @Override
            public int compare(Reservation a, Reservation b) {
                return Integer.compare(b.nights(), a.nights());   // 숙박이 긴 순
            }
        });
        System.out.println("익명 클래스  " + names(reservations));

        // 람다: 메서드가 하나뿐이라 "(매개변수) -> 몸통"만 쓰면 된다
        reservations.sort((a, b) -> Integer.compare(a.nights(), b.nights()));   // 숙박이 짧은 순
        System.out.println("람다         " + names(reservations));

        // 메서드 참조와 Comparator 도우미 메서드
        reservations.sort(Comparator.comparing(Reservation::nights).reversed());   // 다시 긴 순
        System.out.println("메서드 참조  " + names(reservations));

        System.out.println();
        System.out.println("== 2. 자주 쓰는 함수형 인터페이스 ==");
        Predicate<Reservation> longStay = r -> r.nights() >= 2;                     // T → boolean
        Function<Reservation, String> toName = r -> r.guestName();                  // T → R
        Consumer<Reservation> printer = r -> System.out.println("Consumer   " + r.guestName() + " 출력");   // T → 없음
        Supplier<Reservation> sample = () -> new Reservation(9L, "견본", "스위트", 1);   // 없음 → T
        BiFunction<Integer, Integer, Integer> total = (price, nights) -> price * nights;   // (T, U) → R

        System.out.println("Predicate  김민수 2박 이상? " + longStay.test(kim) + ", 박지영 2박 이상? " + longStay.test(park));
        System.out.println("Function   " + toName.apply(lee));
        printer.accept(kim);
        System.out.println("Supplier   " + sample.get());
        System.out.println("BiFunction 180000원 × 3박 = " + total.apply(180000, 3));

        System.out.println();
        System.out.println("== 3. 직접 만든 함수형 인터페이스 ==");
        PriceRule weekend = price -> price * 120 / 100;   // 주말 20% 할증
        PriceRule member = price -> price - 10000;        // 회원 1만 원 할인
        System.out.println("주말        " + weekend.apply(180000));
        System.out.println("주말 + 회원 " + member.apply(weekend.apply(180000)));

        System.out.println();
        System.out.println("== 4. 람다 안에서 바깥 변수 쓰기 ==");
        int minNights = 2;   // 람다가 쓰는 바깥 지역 변수는 값을 바꾸지 않아야 한다
        Predicate<Reservation> atLeast = r -> r.nights() >= minNights;
        // minNights = 3;    // 이 줄의 주석을 풀면 컴파일 오류(실행 결과 맨 아래)
        System.out.println(minNights + "박 이상: " + reservations.stream().filter(atLeast).map(Reservation::guestName).toList());

        System.out.println();
        System.out.println("== 5. 메서드 참조 네 가지 ==");
        Function<Reservation, String> instanceMethod = Reservation::guestName;   // r -> r.guestName()
        Function<String, Integer> staticMethod = Integer::parseInt;             // s -> Integer.parseInt(s)
        Consumer<String> boundMethod = System.out::println;                     // s -> System.out.println(s)
        Supplier<List<String>> constructor = ArrayList::new;                    // () -> new ArrayList<>()

        boundMethod.accept("객체의 메서드  " + instanceMethod.apply(kim));
        boundMethod.accept("static 메서드  " + (staticMethod.apply("180000") + 1));
        List<String> empty = constructor.get();
        boundMethod.accept("생성자         " + empty + " (새 ArrayList)");
    }

    private static List<String> names(List<Reservation> list) {
        return list.stream().map(r -> r.guestName() + "(" + r.nights() + "박)").toList();
    }
}
