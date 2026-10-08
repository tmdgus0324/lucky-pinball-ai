package com.example.reservation.reservation;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// @RestController가 아니라 @Controller. 반환한 문자열이 JSON이 아니라 "화면 이름"이 된다.
// "reservation-list" → templates/reservation-list.html
@Controller
@RequestMapping("/reservations")
public class ReservationPageController {

    private static final List<String> ROOMS = List.of("스탠다드 트윈", "오션뷰 디럭스", "스위트");

    private final ReservationStore store;

    public ReservationPageController(ReservationStore store) {
        this.store = store;
    }

    // 목록 화면. Model에 담은 값을 템플릿에서 ${reservations}로 꺼낸다(JSP의 request.setAttribute와 같은 역할).
    @GetMapping
    public String list(Model model) {
        model.addAttribute("reservations", store.findAll());
        return "reservation-list";
    }

    // 입력 화면. 빈 폼 객체를 넘겨야 th:field가 값을 채울 수 있다.
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ReservationForm());
        model.addAttribute("rooms", ROOMS);
        return "reservation-form";
    }

    // 폼 제출. 검증에 실패하면 같은 화면을 다시 보여 주고(입력값 유지 + 오류 메시지),
    // 성공하면 목록으로 리다이렉트한다(새로고침해도 다시 저장되지 않게. PRG 패턴).
    @PostMapping
    public String create(@Valid @ModelAttribute("form") ReservationForm form, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", ROOMS);
            return "reservation-form";
        }
        Reservation saved = store.save(form.getGuestName(), form.getRoomName(), form.getNights());
        // 리다이렉트 뒤 화면에 한 번만 보여 줄 메시지(세션에 잠깐 담았다가 꺼내면 지워진다)
        redirectAttributes.addFlashAttribute("message", saved.id() + "번 예약이 저장되었습니다.");
        return "redirect:/reservations";
    }
}
