package com.utefit.service.trainer;

import com.utefit.dto.trainer.TrainerDashboardSummaryResponse;

public interface TrainerDashboardService {

    TrainerDashboardSummaryResponse getDashboardForTrainer(Long trainerUserId);
}