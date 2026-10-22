package com.utefit.service;

import com.utefit.dto.trainer.TrainerResponse;
import com.utefit.dto.trainerrequest.TrainerRequestResponse;
import com.utefit.enums.TrainerRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberTrainerRequestService {

    private final MemberAccountService accounts;
    private final TrainerRequestService trainerRequests; // Service do B sở hữu
    private final TrainerService trainers;               // Service do B sở hữu

    public TrainerResponse getTrainer(Long trainerId) {
        return trainers.getPublicTrainerById(trainerId);
    }

    public Page<TrainerRequestResponse> getMyRequests(String email, int page) {
        return trainerRequests.getRequestsForMember(
                accounts.requireMemberId(email),
                PageRequest.of(Math.max(0, page), 10));
    }

    public void sendRequest(String email, Long trainerId, String message) {
        if (trainerId == null || trainerId <= 0) {
            throw new IllegalArgumentException("PT không hợp lệ.");
        }
        String clean = message == null ? "" : message.trim();
        if (clean.length() > 2000) {
            throw new IllegalArgumentException("Lời nhắn tối đa 2000 ký tự.");
        }
        trainerRequests.createRequest(accounts.requireMemberId(email),
                trainerId, clean.isBlank() ? null : clean);
    }

    public void cancelRequest(String email, Long requestId) {
        trainerRequests.cancelPendingRequest(accounts.requireMemberId(email), requestId);
    }

    public List<AcceptedTrainer> getAcceptedTrainers(String email) {
        Long memberId = accounts.requireMemberId(email);
        LinkedHashMap<Long, AcceptedTrainer> unique = new LinkedHashMap<>();
        for (TrainerRequestResponse request : trainerRequests
                .getRequestsForMember(memberId, Pageable.unpaged()).getContent()) {
            if (request.getStatus() == TrainerRequestStatus.ACCEPTED) {
                unique.putIfAbsent(request.getTrainerId(),
                        new AcceptedTrainer(request.getTrainerId(),
                                request.getTrainerName(), request.getTrainerEmail()));
            }
        }
        return List.copyOf(unique.values());
    }

    public record AcceptedTrainer(Long id, String name, String email) {}
}
