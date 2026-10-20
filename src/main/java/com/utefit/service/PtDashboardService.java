package com.utefit.service;

import com.utefit.dto.trainer.PtDashboardResponse;

public interface PtDashboardService {

    PtDashboardResponse getDashboardForTrainer(Long trainerUserId);
}