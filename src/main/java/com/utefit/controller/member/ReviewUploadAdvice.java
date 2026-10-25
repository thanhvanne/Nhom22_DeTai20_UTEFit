package com.utefit.controller.member;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = MemberReviewController.class)
public class ReviewUploadAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "File quá lớn. Video tối đa 20 MB, ảnh tối đa 5 MB; tổng request tối đa 26 MB.");
        return "redirect:/member/reviews";
    }
}
