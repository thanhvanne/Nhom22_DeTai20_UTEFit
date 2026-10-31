
package com.utefit.dto.member;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembershipPurchaseRequest {
    @jakarta.validation.constraints.Pattern(regexp = "AUTO|NONE", message = "Lựa chọn ưu đãi không hợp lệ")
    @com.utefit.validation.ValidInput("offerSelection")
    private String offerSelection = "AUTO";
    @jakarta.validation.constraints.Size(max = 50)
    @com.utefit.validation.ValidInput("promotionCode")
    private String promotionCode;

    @NotNull(message = "Vui lòng chọn chi nhánh")
    @com.utefit.validation.ValidInput("id")
    private Long branchId;

    @NotNull(message = "Vui lòng chọn gói tập")
    @com.utefit.validation.ValidInput("id")
    private Long planId;
}
