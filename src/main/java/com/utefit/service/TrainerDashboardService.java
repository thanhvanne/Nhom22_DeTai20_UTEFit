package com.utefit.service;

import com.utefit.dto.trainer.TrainerDashboardSummaryResponse;

public interface TrainerDashboardService {

    TrainerDashboardSummaryResponse getDashboardForTrainer(Long trainerUserId);
}