
package com.utefit.dto.member;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembershipPurchaseRequest {

    @NotNull(message = "Vui lòng chọn chi nhánh")
    private Long branchId;

    @NotNull(message = "Vui lòng chọn gói tập")
    private Long planId;
}
