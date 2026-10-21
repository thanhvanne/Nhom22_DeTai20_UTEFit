package com.utefit.controller.pt;

import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;
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
import java.util.List;

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
     * Danh sách lịch PT 1:1 của HLV đang đăng nhập.
     * URL: /pt/personal-schedules?fromDate=yyyy-MM-dd&toDate=yyyy-MM-dd (optional)
     */
    @GetMapping
    public String listSchedules(@RequestParam(value = "fromDate", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                @RequestParam(value = "toDate", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                Principal principal,
                                Model model) {

        User trainerUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được tài khoản HLV"));

        LocalDate today = LocalDate.now();
        if (fromDate == null) {
            fromDate = today;
        }
        if (toDate == null) {
            toDate = today.plusDays(14); // mặc định xem 2 tuần tới
        }

        List<PersonalTrainingScheduleResponse> schedules =
                personalTrainingScheduleService.getSchedulesForTrainer(trainerUser.getId(), fromDate, toDate);

        model.addAttribute("schedules", schedules);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "pt/personal-schedules/list";
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
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hội viên"));

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

        User trainerUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được tài khoản HLV"));

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
            model.addAttribute("errorMessage", ex.getMessage());
        }

        // Load lại thông tin member cho view
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hội viên"));
        model.addAttribute("member", member);

        return "pt/personal-schedules/create";
    }
}