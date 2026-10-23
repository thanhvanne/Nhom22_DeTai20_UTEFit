package com.utefit.controller.pt;

import com.utefit.dto.trainer.PtClassScheduleDetailResponse;
import com.utefit.enums.RegistrationStatus;
import com.utefit.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/pt/classes")
@RequiredArgsConstructor
public class PtClassController {
    private final PtClassService classes;
    private final PtAttendanceService attendance;
    private final PtPersonalScheduleViewService ptUsers;
    private final PersonalTrainingScheduleService personal;
    private final WeeklyTimetableService timetable;

    @GetMapping
    public String week(Authentication auth,
                       @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate weekStart,
                       Model model) {
        model.addAttribute("week",timetable.trainerWeek(auth.getName(),weekStart));
        return "pt/classes/list";
    }
    @GetMapping("/{scheduleId}")
    public String detail(Authentication auth,@PathVariable Long scheduleId,Model model) {
        PtClassScheduleDetailResponse result=classes.getScheduleDetailForTrainer(
                ptUsers.trainerUserId(auth.getName()),scheduleId);
        model.addAttribute("schedule",result);
        return "pt/classes/detail";
    }
    @PostMapping("/{scheduleId}/attendance/{registrationId}")
    public String attendance(@PathVariable Long scheduleId,@PathVariable Long registrationId,
                             @RequestParam RegistrationStatus result,Authentication auth,RedirectAttributes redirect) {
        try { attendance.record(auth.getName(),scheduleId,registrationId,result);
            redirect.addFlashAttribute("successMessage","Đã điểm danh.");
        } catch (IllegalArgumentException|IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage",ex.getMessage());
        }
        return "redirect:/pt/classes/"+scheduleId;
    }
    @PostMapping("/personal/{id}/{action}")
    public String personalAction(@PathVariable Long id,@PathVariable String action,
                                 @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate weekStart,
                                 Authentication auth,RedirectAttributes redirect) {
        Long ptUserId=ptUsers.trainerUserId(auth.getName());
        try {
            switch (action) {
                case "cancel" -> personal.cancelSchedule(ptUserId,id);
                case "complete" -> personal.completeSchedule(ptUserId,id);
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            redirect.addFlashAttribute("successMessage","Đã cập nhật buổi PT.");
        } catch (IllegalArgumentException|IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage",ex.getMessage());
        }
        return "redirect:/pt/classes?weekStart="+weekStart;
    }
}
