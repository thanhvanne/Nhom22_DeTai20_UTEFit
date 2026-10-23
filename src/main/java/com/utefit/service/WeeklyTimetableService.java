package com.utefit.service;

import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;
import com.utefit.dto.trainer.PtClassScheduleSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WeeklyTimetableService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final MemberClassBookingService bookings;
    private final MemberPersonalScheduleService memberPt;
    private final PtClassService ptClasses;
    private final PersonalTrainingScheduleService ptSessions;
    private final PtPersonalScheduleViewService ptViews;

    public record Slot(String kind, String title, String detail,
                       LocalTime start, LocalTime end, String status,
                       String detailUrl, String cancelUrl, String completeUrl) {}
    public record Day(LocalDate date, String label, List<Slot> slots) {}
    public record Week(LocalDate start, LocalDate end, LocalDate today,
                       List<Day> days, boolean hasEntries) {
        public LocalDate previous() { return start.minusWeeks(1); }
        public LocalDate next() { return start.plusWeeks(1); }
    }

    private LocalDate start(LocalDate date) {
        LocalDate chosen = date == null ? LocalDate.now(ZONE) : date;
        // Tránh truy vấn ngày quá xa bằng URL tự tạo.
        if (chosen.isBefore(LocalDate.now(ZONE).minusYears(1))
                || chosen.isAfter(LocalDate.now(ZONE).plusYears(1))) {
            throw new IllegalArgumentException("Chỉ xem lịch trong phạm vi 1 năm quanh hiện tại.");
        }
        return chosen.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
    private Week week(LocalDate begin, Map<LocalDate,List<Slot>> data) {
        List<Day> days = new ArrayList<>();
        String[] labels = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
        for (int i=0; i<7; i++) {
            LocalDate day=begin.plusDays(i);
            List<Slot> slots = new ArrayList<>(data.getOrDefault(day, List.of()));
            slots.sort(Comparator.comparing(Slot::start));
            days.add(new Day(day, labels[i], slots));
        }
        return new Week(begin,begin.plusDays(6),LocalDate.now(ZONE),days,
                days.stream().anyMatch(d -> !d.slots().isEmpty()));
    }
    private static void add(Map<LocalDate,List<Slot>> map, LocalDate date, Slot slot) {
        map.computeIfAbsent(date, d -> new ArrayList<>()).add(slot);
    }

    @Transactional(readOnly = true)
    public Week memberWeek(String email, LocalDate date) {
        LocalDate begin=start(date), end=begin.plusDays(6);
        Map<LocalDate,List<Slot>> data=new HashMap<>();
        for (MemberClassBookingService.BookingView b: bookings.mySchedule(email)) {
            if (b.date()==null || b.date().isBefore(begin) || b.date().isAfter(end)) continue;
            add(data,b.date(),new Slot("CLASS",b.className(), b.branchName() + " · " + (b.room()==null?"Chưa có phòng":b.room()),
                    b.startTime(),b.endTime(),b.status(),"/member/schedule",
                    b.canCancel()?"/member/classes/"+b.scheduleId()+"/cancel":null,null));
        }
        for (PersonalTrainingScheduleResponse s: memberPt.getMySchedules(email,begin,end)) {
            add(data,s.getScheduleDate(),new Slot("PT",s.getTitle()==null?"Buổi PT 1:1":s.getTitle(),
                    "PT: "+s.getTrainerName()+" · "+s.getBranchName(),s.getStartTime(),s.getEndTime(),s.getStatus(),
                    "/member/pt-schedules",null,null));
        }
        return week(begin,data);
    }

    @Transactional(readOnly = true)
    public Week trainerPersonalWeek(String email, LocalDate date) {
        Week all = trainerWeek(email, date);
        List<Day> days = all.days().stream()
                .map(d -> new Day(d.date(),d.label(),d.slots().stream()
                        .filter(slot -> "PT".equals(slot.kind())).toList()))
                .toList();
        return new Week(all.start(),all.end(),all.today(),days,
                days.stream().anyMatch(d -> !d.slots().isEmpty()));
    }

    @Transactional(readOnly = true)
    public Week trainerWeek(String email, LocalDate date) {
        Long id=ptViews.trainerUserId(email);
        LocalDate begin=start(date), end=begin.plusDays(6);
        Map<LocalDate,List<Slot>> data=new HashMap<>();
        // API hiện tại lọc từ đầu tuần, đặt page đủ lớn cho bảng lịch hàng tuần.
        for (PtClassScheduleSummaryResponse s:ptClasses.getSchedulesForTrainer(id,begin,PageRequest.of(0,500)).getContent()) {
            if (s.getScheduleDate().isAfter(end)) continue;
            add(data,s.getScheduleDate(),new Slot("CLASS",s.getClassName(),
                    s.getBranchName()+" · "+s.getRoom(),s.getStartTime(),s.getEndTime(),"Được phân công",
                    "/pt/classes/"+s.getScheduleId(),null,null));
        }
        LocalDateTime now=LocalDateTime.now(ZONE);
        for (PersonalTrainingScheduleResponse s:ptSessions.getSchedulesForTrainer(id,begin,end)) {
            boolean scheduled="SCHEDULED".equals(s.getStatus());
            LocalDateTime begins=s.getScheduleDate().atTime(s.getStartTime());
            LocalDateTime finishes=s.getScheduleDate().atTime(s.getEndTime());
            String base="/pt/classes/personal/"+s.getId();
            add(data,s.getScheduleDate(),new Slot("PT",s.getTitle()==null?"PT 1:1":s.getTitle(),
                    "Member: "+s.getMemberName(),s.getStartTime(),s.getEndTime(),s.getStatus(),
                    "/pt/personal-schedules",
                    scheduled&&begins.isAfter(now)?base+"/cancel":null,
                    scheduled&&!finishes.isAfter(now)?base+"/complete":null));
        }
        return week(begin,data);
    }
}
