package com.utefit.service;

import com.utefit.dto.trainer.PtClassScheduleDetailResponse;
import com.utefit.dto.trainer.PtClassScheduleSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PtClassService {

    Page<PtClassScheduleSummaryResponse> getSchedulesForTrainer(Long trainerUserId,
                                                                LocalDate fromDate,
                                                                Pageable pageable);

    PtClassScheduleDetailResponse getScheduleDetailForTrainer(Long trainerUserId,
                                                              Long scheduleId);
}