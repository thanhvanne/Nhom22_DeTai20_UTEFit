package com.utefit.controller.member;

import com.utefit.service.MemberPersonalScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/member/pt-schedules")
@RequiredArgsConstructor
public class MemberPersonalScheduleController {

    private final MemberPersonalScheduleService schedules;

    @GetMapping
    public String list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal, Model model) {

        LocalDate today = LocalDate.now();
        LocalDate start = from == null ? today.minusMonths(1) : from;
        LocalDate end = to == null ? today.plusMonths(6) : to;

        try {
            model.addAttribute("schedules", schedules.getMySchedules(
                    principal.getName(), start, end));
        } catch (IllegalArgumentException ex) {
            // Không redirect lại cùng tham số sai để tránh vòng lặp.
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("schedules", java.util.List.of());
        }
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "member/pt-schedules";
    }
}