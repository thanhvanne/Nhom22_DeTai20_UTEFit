package com.utefit.dto.member;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewForm {

    @NotNull(message = "Vui lòng chọn chi nhánh.")
    @com.utefit.validation.ValidInput("id")
    private Long branchId;

    // null = đánh giá chi nhánh; có ID = đánh giá PT.
    @com.utefit.validation.ValidInput("id")
    private Long trainerId;

    @NotNull(message = "Vui lòng chọn số sao.")
    @Min(value = 1, message = "Điểm thấp nhất là 1 sao.")
    @Max(value = 5, message = "Điểm cao nhất là 5 sao.")
    private Integer rating;

    @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự.")
    @com.utefit.validation.ValidInput("comment")
    private String comment;

    private boolean removeVideo;
}
