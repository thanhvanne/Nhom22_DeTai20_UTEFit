package com.utefit.service.impl;

import com.utefit.repository.BranchRepository;
import com.utefit.repository.FitnessClassRepository;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.UserRepository;
import com.utefit.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final TrainerRepository trainerRepository;
    private final FitnessClassRepository fitnessClassRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getBasicKpiCounts() {
        Map<String, Long> kpis = new HashMap<>();
        kpis.put("totalUsers", userRepository.count());
        kpis.put("totalBranches", branchRepository.count());
        kpis.put("totalTrainers", trainerRepository.count());
        kpis.put("totalClasses", fitnessClassRepository.count());
        return kpis;
    }
}