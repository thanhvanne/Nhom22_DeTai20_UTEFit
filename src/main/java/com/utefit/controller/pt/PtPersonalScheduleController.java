package com.utefit.controller.pt;

import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
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

@Controller
@RequestMapping("/pt/personal-schedules")
public class PtPersonalScheduleController {

    private final PersonalTrainingScheduleService personalTrainingScheduleService;
    private final UserRepository userRepository;

    public PtPersonalScheduleController(PersonalTrainingScheduleService personalTrainingScheduleService,
                                        UserRepository userRepository) {
        this.personalTrainingScheduleService = personalTrainingScheduleService;
        this.userRepository = userRepository;
    }

    /**
     * Hiển thị form tạo lịch PT cá nhân cho một Member cụ thể.
     * URL: /pt/personal-schedules/create?memberId=...
     */
    @GetMapping("/create")
    public String showCreateForm(@RequestParam("memberId") Long memberId,
                                 Principal principal,
                                 Model model) {

        // Lấy thông tin Member để hiển thị trên form
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hội viên"));

        model.addAttribute("member", member);

        return "pt/personal-schedules/create";
    }

    /**
     * Xử lý submit tạo lịch PT cá nhân.
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
                                 @RequestParam(name = "title", required = false)
                                 String title,
                                 @RequestParam(name = "note", required = false)
                                 String note,
                                 Principal principal,
                                 Model model) {

        // Lấy user PT hiện tại
        User trainerUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được tài khoản HLV"));

        if (title == null || title.isBlank()) {
            title = "Buổi tập cá nhân";
        }

        try {
            personalTrainingScheduleService.createScheduleForMember(
                    trainerUser.getId(),
                    memberId,
                    scheduleDate,
                    startTime,
                    endTime,
                    title,
                    note
            );
            model.addAttribute("successMessage", "Tạo lịch tập cá nhân thành công.");
        } catch (RuntimeException ex) {
            // Service đã validate start < end, trùng lịch PT/Member, TrainerRequest ACCEPTED...
            model.addAttribute("errorMessage", ex.getMessage());
        }

        // Load lại thông tin member cho view
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hội viên"));
        model.addAttribute("member", member);

        return "pt/personal-schedules/create";
    }
}