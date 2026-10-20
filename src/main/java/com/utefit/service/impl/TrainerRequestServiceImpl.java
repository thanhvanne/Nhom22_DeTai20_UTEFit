package com.utefit.service.impl;

import com.utefit.dto.trainerrequest.TrainerRequestResponse;
import com.utefit.entity.Trainer;
import com.utefit.entity.TrainerRequest;
import com.utefit.entity.User;
import com.utefit.enums.TrainerRequestStatus;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.TrainerRequestRepository;
import com.utefit.repository.UserRepository;
import com.utefit.service.TrainerRequestService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainerRequestServiceImpl implements TrainerRequestService {

    private final TrainerRequestRepository trainerRequestRepository;
    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;

    @Override
    public TrainerRequestResponse createRequest(Long memberUserId,
                                                Long trainerId,
                                                String message) {

        User member = userRepository.findById(memberUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy thành viên"));

        if (member.getRole() != UserRole.MEMBER) {
            throw new IllegalStateException("Chỉ thành viên mới được gửi yêu cầu PT.");
        }

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy huấn luyện viên."));

        User trainerUser = trainer.getUser();

        // Lấy trạng thái active/enabled an toàn cho cả Boolean và boolean
        boolean trainerActive = Boolean.TRUE.equals(trainer.getActive());
        boolean trainerUserEnabled = Boolean.TRUE.equals(trainerUser.getEnabled());

        // Chặn PT inactive / user khoá / không phải PT
        if (!trainerActive
                || !trainerUserEnabled
                || trainerUser.getStatus() != UserStatus.ACTIVE
                || trainerUser.getRole() != UserRole.PT) {
            throw new IllegalStateException("Huấn luyện viên hiện không khả dụng.");
        }

        // Chặn duplicate PENDING
        boolean existedPending = trainerRequestRepository
                .existsByMember_IdAndTrainer_IdAndStatus(
                        memberUserId,
                        trainerId,
                        TrainerRequestStatus.PENDING
                );

        if (existedPending) {
            throw new IllegalStateException(
                    "Bạn đã có một yêu cầu đang chờ phản hồi với huấn luyện viên này."
            );
        }

        TrainerRequest entity = TrainerRequest.builder()
                .member(member)
                .trainer(trainer)
                .message(message)
                .status(TrainerRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        TrainerRequest saved = trainerRequestRepository.save(entity);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainerRequestResponse> getRequestsForMember(Long memberUserId,
                                                             Pageable pageable) {
        return trainerRequestRepository
                .findByMember_IdOrderByRequestedAtDesc(memberUserId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainerRequestResponse> getRequestsForTrainer(Long trainerUserId,
                                                              Pageable pageable) {
        return trainerRequestRepository
                .findByTrainer_User_IdOrderByRequestedAtDesc(trainerUserId, pageable)
                .map(this::toResponse);
    }

    @Override
    public void acceptRequest(Long trainerUserId, Long requestId) {
        changeStatus(trainerUserId, requestId, TrainerRequestStatus.ACCEPTED);
    }

    @Override
    public void rejectRequest(Long trainerUserId, Long requestId) {
        changeStatus(trainerUserId, requestId, TrainerRequestStatus.REJECTED);
    }

    private void changeStatus(Long trainerUserId,
                              Long requestId,
                              TrainerRequestStatus targetStatus) {

        TrainerRequest request = trainerRequestRepository
                .findByIdAndTrainer_User_Id(requestId, trainerUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy yêu cầu hoặc bạn không có quyền xử lý."));

        if (request.getStatus() != TrainerRequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó.");
        }

        request.setStatus(targetStatus);
        request.setRespondedAt(LocalDateTime.now());
        // @Transactional sẽ tự flush
    }

    private TrainerRequestResponse toResponse(TrainerRequest entity) {
        User member = entity.getMember();
        Trainer trainer = entity.getTrainer();
        User trainerUser = trainer.getUser();

        String memberName = buildFullName(member.getLastName(), member.getFirstName());
        String trainerName = buildFullName(trainerUser.getLastName(), trainerUser.getFirstName());

        return TrainerRequestResponse.builder()
                .id(entity.getId())

                .memberId(member.getId())
                .memberName(memberName)
                .memberEmail(member.getEmail())

                .trainerId(trainer.getId())
                .trainerName(trainerName)
                .trainerEmail(trainerUser.getEmail())

                .message(entity.getMessage())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .respondedAt(entity.getRespondedAt())
                .build();
    }

    private String buildFullName(String lastName, String firstName) {
        String ln = lastName != null ? lastName.trim() : "";
        String fn = firstName != null ? firstName.trim() : "";
        return (ln + " " + fn).trim();
    }
}