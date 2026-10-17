package com.utefit.service;

import com.utefit.dto.trainer.BranchOptionResponse;
import com.utefit.dto.trainer.TrainerProfileUpdateRequest;
import com.utefit.dto.trainer.TrainerResponse;
import com.utefit.dto.trainer.TrainerSearchRequest;

import org.springframework.data.domain.Page;

import java.util.List;

public interface TrainerService {

    Page<TrainerResponse> searchPublicTrainers(
            TrainerSearchRequest request
    );

    TrainerResponse getPublicTrainerById(Long id);

    List<BranchOptionResponse> getActiveBranchOptions();

    TrainerResponse getMyProfile(Long userId);

    TrainerResponse updateMyProfile(
            Long userId,
            TrainerProfileUpdateRequest request
    );
}