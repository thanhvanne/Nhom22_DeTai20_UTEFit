package com.utefit.controller.member;

import com.utefit.service.MemberClassBookingService;
import com.utefit.service.PublicCatalogService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.utefit.service.WeeklyTimetableService;

@Controller
@RequestMapping("/member")
public class MemberClassBookingController {

    private final MemberClassBookingService booking;
    private final PublicCatalogService catalog;
    private final WeeklyTimetableService timetable;

    public MemberClassBookingController(
            MemberClassBookingService booking,
            PublicCatalogService catalog,
            WeeklyTimetableService timetable
    ) {
        this.booking = booking;
        this.catalog = catalog;
        this.timetable = timetable;
    }

    // Danh sách lịch lớp, tìm kiếm và lọc.
    @GetMapping("/classes")
    public String classes(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "schedules",
                booking.availableSchedules(
                        principal.getName(),
                        keyword,
                        branchId,
                        page
                )
        );

        model.addAttribute(
                "branches",
                catalog.activeBranchOptions()
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("branchId", branchId);

        return "member/classes";
    }

    // Đăng ký một buổi học cụ thể.
    @PostMapping("/classes/{id}/book")
    public String book(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            booking.book(
                    principal.getName(),
                    id
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đăng ký lịch lớp thành công."
            );

            return "redirect:/member/schedule";

        } catch (IllegalArgumentException | IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

            return "redirect:/member/classes";
        }
    }

    // Hủy đăng ký.
    @PostMapping("/classes/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            booking.cancel(
                    principal.getName(),
                    id
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đã hủy đăng ký lịch lớp."
            );

        } catch (IllegalArgumentException | IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/member/schedule" + (weekStart == null ? "" : "?weekStart=" + weekStart);
    }

    // Lịch sử và lịch cá nhân.
    @GetMapping("/schedule")
    public String schedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Principal principal, Model model
    ) {
        model.addAttribute("week", timetable.memberWeek(principal.getName(), weekStart));
        return "member/schedule";
    }
}
