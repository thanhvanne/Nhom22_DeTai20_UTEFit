package com.utefit.controller.pt;

import com.utefit.dto.trainer.PtClassScheduleDetailResponse;
import com.utefit.dto.trainer.PtClassScheduleSummaryResponse;
import com.utefit.dto.trainer.PtWeeklyDayScheduleResponse;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import com.utefit.service.PtClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pt/classes")
public class PtClassController {

    private final PtClassService ptClassService;
    private final UserRepository userRepository;

    public PtClassController(PtClassService ptClassService,
                             UserRepository userRepository) {
        this.ptClassService = ptClassService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản đăng nhập."));
        return user.getId();
    }

    // Lịch lớp dạng tuần cho PT
    @GetMapping
    public String listWeeklyClasses(Authentication authentication,
                                    @RequestParam(value = "weekStart", required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartParam,
                                    Model model) {

        Long userId = getCurrentUserId(authentication);
        LocalDate today = LocalDate.now();

        // Nếu không truyền weekStart -> lấy tuần hiện tại
        LocalDate baseDate = (weekStartParam != null ? weekStartParam : today);

        // Quy về thứ 2 của tuần chứa baseDate
        LocalDate weekStart = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        // Lấy lịch từ weekStart trở đi (giới hạn 200 bản ghi), rồi lọc lại trong tuần
        Pageable pageable = PageRequest.of(0, 200);
        Page<PtClassScheduleSummaryResponse> page =
                ptClassService.getSchedulesForTrainer(userId, weekStart, pageable);

        final LocalDate filterStart = weekStart;
        final LocalDate filterEnd = weekEnd;

        List<PtClassScheduleSummaryResponse> weekSchedules = page.getContent().stream()
                .filter(s -> {
                    LocalDate d = s.getScheduleDate();
                    return !d.isBefore(filterStart) && !d.isAfter(filterEnd);
                })
                .toList();

        // Chuẩn bị cấu trúc 7 ngày trong tuần
        List<PtWeeklyDayScheduleResponse> weekDays = new ArrayList<>();
        Map<LocalDate, PtWeeklyDayScheduleResponse> dayMap = new HashMap<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            PtWeeklyDayScheduleResponse dayView = new PtWeeklyDayScheduleResponse();
            dayView.setDate(date);
            dayView.setDayLabel(toVietnameseDayOfWeek(date));

            weekDays.add(dayView);
            dayMap.put(date, dayView);
        }

        // Phân lớp vào các ô Sáng / Chiều / Tối của từng ngày
        for (PtClassScheduleSummaryResponse schedule : weekSchedules) {
            LocalDate date = schedule.getScheduleDate();
            PtWeeklyDayScheduleResponse dayView = dayMap.get(date);
            if (dayView == null) {
                continue;
            }

            LocalTime start = schedule.getStartTime();
            if (start.isBefore(LocalTime.NOON)) {
                dayView.getMorningClasses().add(schedule);
            } else if (start.isBefore(LocalTime.of(18, 0))) {
                dayView.getAfternoonClasses().add(schedule);
            } else {
                dayView.getEveningClasses().add(schedule);
            }
        }

        boolean hasSchedules = !weekSchedules.isEmpty();

        model.addAttribute("today", today);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("hasSchedules", hasSchedules);

        return "pt/classes/list";
    }

    // Chi tiết một lịch lớp + danh sách học viên
    @GetMapping("/{scheduleId}")
    public String viewSchedule(Authentication authentication,
                               @PathVariable("scheduleId") Long scheduleId,
                               Model model) {

        Long userId = getCurrentUserId(authentication);

        PtClassScheduleDetailResponse schedule =
                ptClassService.getScheduleDetailForTrainer(userId, scheduleId);

        model.addAttribute("schedule", schedule);

        return "pt/classes/detail";
    }

    // Helper: chuyển LocalDate -> "Thứ 2", "Thứ 3", ..., "Chủ nhật"
    private String toVietnameseDayOfWeek(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ nhật";
        };
    }
}