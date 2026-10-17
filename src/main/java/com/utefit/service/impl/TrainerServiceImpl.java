package com.utefit.service.impl;

import com.utefit.dto.trainer.BranchOptionResponse;
import com.utefit.dto.trainer.TrainerProfileUpdateRequest;
import com.utefit.dto.trainer.TrainerResponse;
import com.utefit.dto.trainer.TrainerSearchRequest;
import com.utefit.entity.Branch;
import com.utefit.entity.Trainer;
import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.BranchRepository;
import com.utefit.repository.TrainerRepository;
import com.utefit.service.TrainerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TrainerServiceImpl implements TrainerService {

    private final TrainerRepository trainerRepository;
    private final BranchRepository branchRepository;

    public TrainerServiceImpl(TrainerRepository trainerRepository,
                              BranchRepository branchRepository) {
        this.trainerRepository = trainerRepository;
        this.branchRepository = branchRepository;
    }

   
    @Override
    @Transactional(readOnly = true)
    public Page<TrainerResponse> searchPublicTrainers(TrainerSearchRequest request) {

        if (request == null) {
            request = new TrainerSearchRequest();
        }

        String keyword = normalize(request.getKeyword());

        int page = request.getPage() == null
                ? 0
                : Math.max(0, request.getPage());

        int size = 6;

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")  
        );

        return trainerRepository
                .searchPublicTrainers(
                        keyword,
                        request.getBranchId(),
                        UserStatus.ACTIVE,
                        UserRole.PT,
                        pageable
                )
                .map(this::toResponse);
    }

   
    @Override
    @Transactional(readOnly = true)
    public TrainerResponse getPublicTrainerById(Long id) {

        Trainer trainer = trainerRepository
                .findPublicTrainerById(
                        id,
                        UserStatus.ACTIVE,
                        UserRole.PT
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy huấn luyện viên"
                ));

        return toResponse(trainer);
    }

    
    @Override
    @Transactional(readOnly = true)
    public List<BranchOptionResponse> getActiveBranchOptions() {

        return branchRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(branch -> new BranchOptionResponse(
                        branch.getId(),
                        branch.getName()
                ))
                .toList();
    }

    
    @Override
    @Transactional(readOnly = true)
    public TrainerResponse getMyProfile(Long userId) {

        Trainer trainer = trainerRepository
                .findByUserId(userId, UserRole.PT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ PT"
                ));

        return toResponse(trainer);
    }

   
    @Override
    @Transactional
    public TrainerResponse updateMyProfile(Long userId,
                                           TrainerProfileUpdateRequest request) {

        Trainer trainer = trainerRepository
                .findByUserId(userId, UserRole.PT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ PT"
                ));

        trainer.setSpecialization(normalize(request.getSpecialization()));
        trainer.setYearsExperience(request.getYearsExperience());
        trainer.setBio(normalize(request.getBio()));

        trainerRepository.save(trainer);

        return toResponse(trainer);
    }

    
    private TrainerResponse toResponse(Trainer trainer) {

        User user = trainer.getUser();
        Branch branch = trainer.getBranch();

        return TrainerResponse.builder()
                .id(trainer.getId())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .specialization(trainer.getSpecialization())
                .yearsExperience(trainer.getYearsExperience())
                .bio(trainer.getBio())
                .branchId(branch != null ? branch.getId() : null)
                .branchName(branch != null ? branch.getName() : null)
                .branchAddress(branch != null ? branch.getAddress() : null)
                .build();
    }

    
    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}