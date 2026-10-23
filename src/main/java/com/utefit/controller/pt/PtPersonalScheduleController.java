package com.utefit.controller.pt;

import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;
import com.utefit.entity.User;
import com.utefit.service.PtPersonalScheduleViewService;
import com.utefit.service.WeeklyTimetableService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.utefit.service.PersonalTrainingScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/pt/personal-schedules")
public class PtPersonalScheduleController {

    private final PersonalTrainingScheduleService personalTrainingScheduleService;
    private final PtPersonalScheduleViewService viewService;
    private final WeeklyTimetableService timetable;

    public PtPersonalScheduleController(PersonalTrainingScheduleService personalTrainingScheduleService,
                                        PtPersonalScheduleViewService viewService,
                                        WeeklyTimetableService timetable) {
        this.personalTrainingScheduleService = personalTrainingScheduleService;
        this.viewService = viewService;
        this.timetable = timetable;
    }

    /**
     * Danh sách lịch PT 1:1 của HLV đang đăng nhập.
     * URL: /pt/personal-schedules?fromDate=yyyy-MM-dd&toDate=yyyy-MM-dd (optional)
     */
    @GetMapping
    public String listSchedules(
            @RequestParam(value="weekStart",required=false)
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Principal principal,Model model) {
        model.addAttribute("week",timetable.trainerPersonalWeek(principal.getName(),weekStart));
        return "pt/personal-schedules/list";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            personalTrainingScheduleService.cancelSchedule(viewService.trainerUserId(principal.getName()), id);
            redirect.addFlashAttribute("successMessage", "Đã hủy lịch PT.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/pt/personal-schedules";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            personalTrainingScheduleService.completeSchedule(viewService.trainerUserId(principal.getName()), id);
            redirect.addFlashAttribute("successMessage", "Đã xác nhận hoàn thành buổi PT.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/pt/personal-schedules";
    }

    /**
     * Form tạo lịch PT 1:1 cho một Member cụ thể.
     * URL: /pt/personal-schedules/create?memberId=...
     */
    @GetMapping("/create")
    public String showCreateForm(@RequestParam("memberId") Long memberId,
                                 Principal principal,
                                 Model model) {

        // Lấy thông tin Member để hiển thị trên form
        User member = viewService.member(memberId);

        model.addAttribute("member", member);

        return "pt/personal-schedules/create";
    }

    /**
     * Xử lý submit tạo lịch PT 1:1.
     */
    @PostMapping("/create")
    public String createSchedule(@RequestParam("memberId") Long memberId,
                                 @RequestParam("scheduleDate")
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                 LocalDate scheduleDate,
                                 @RequestParam("startTime")
                                 @DateTimeFormat(pattern = "HH:mm")
                                 LocalTime startTime,
                                 @RequestParam("endTime")
                                 @DateTimeFormat(pattern = "HH:mm")
                                 LocalTime endTime,
                                 @RequestParam(name = "title", required = false) String title,
                                 @RequestParam(name = "note", required = false) String note,
                                 Principal principal,
                                 Model model) {

        Long trainerUserId = viewService.trainerUserId(principal.getName());

        if (title == null || title.isBlank()) {
            title = "Buổi tập cá nhân";
        }

        try {
            personalTrainingScheduleService.createScheduleForMember(
                    trainerUserId,
                    memberId,
                    scheduleDate,
                    startTime,
                    endTime,
                    title,
                    note
            );
            model.addAttribute("successMessage", "Tạo lịch tập cá nhân thành công.");
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }

        // Load lại thông tin member cho view
        User member = viewService.member(memberId);
        model.addAttribute("member", member);

        return "pt/personal-schedules/create";
    }
}
