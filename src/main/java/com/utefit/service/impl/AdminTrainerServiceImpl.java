package com.utefit.service.impl;

import com.utefit.entity.Branch;
import com.utefit.entity.Trainer;
import com.utefit.repository.BranchRepository;
import com.utefit.repository.TrainerRepository;
import com.utefit.service.AdminTrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTrainerServiceImpl implements AdminTrainerService {

    private final TrainerRepository trainerRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Trainer> getTrainers(String keyword, Long branchId, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        String searchPattern = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        return trainerRepository.searchAdminTrainers(searchPattern, branchId, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Trainer getTrainerById(Long id) {
        return trainerRepository.findAdminTrainerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy huấn luyện viên với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Override
    @Transactional
    public void assignBranch(Long trainerId, Long branchId) {
        Trainer trainer = getTrainerById(trainerId);

        if (branchId == null) {
            trainer.setBranch(null);
        } else {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + branchId));
            trainer.setBranch(branch);
        }

        trainerRepository.save(trainer);
    }

    @Override
    @Transactional
    public void toggleActive(Long trainerId) {
        Trainer trainer = getTrainerById(trainerId);
        trainer.setActive(!Boolean.TRUE.equals(trainer.getActive()));
        trainerRepository.save(trainer);
    }
}