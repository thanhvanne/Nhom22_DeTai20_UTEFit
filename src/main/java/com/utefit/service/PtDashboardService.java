package com.utefit.service;

import com.utefit.dto.trainer.TrainerDashboardSummaryResponse;

public interface PtDashboardService {

    TrainerDashboardSummaryResponse getDashboardForTrainer(Long trainerUserId);
}