package com.utefit.service;

import com.utefit.dto.trainerrequest.TrainerRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainerRequestService {

    /**
     * Member gửi request tới một PT
     */
    TrainerRequestResponse createRequest(Long memberUserId,
                                         Long trainerId,
                                         String message);

    /**
     * Lịch sử request của member hiện tại
     */
    Page<TrainerRequestResponse> getRequestsForMember(Long memberUserId,
                                                      Pageable pageable);

    /**
     * Danh sách request gửi tới PT hiện tại
     */
    Page<TrainerRequestResponse> getRequestsForTrainer(Long trainerUserId,
                                                       Pageable pageable);

    /**
     * PT chấp nhận request
     */
    void acceptRequest(Long trainerUserId, Long requestId);

    /**
     * PT từ chối request
     */
    void rejectRequest(Long trainerUserId, Long requestId);
}