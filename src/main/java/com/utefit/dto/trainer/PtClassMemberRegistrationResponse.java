package com.utefit.dto.trainer;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PtClassMemberRegistrationResponse {

    private Long registrationId;
    private Long memberId;
    private String memberName;
    private String memberEmail;

    private LocalDateTime registeredAt;
}
