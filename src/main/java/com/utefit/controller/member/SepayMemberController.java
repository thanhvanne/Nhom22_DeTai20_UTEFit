package com.utefit.controller.member;

import com.utefit.service.SepayPaymentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class SepayMemberController {
    private final SepayPaymentService payments;

    @PostMapping("/member/memberships/{id}/sepay")
    public String start(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            return "redirect:/member/payments/" + payments.start(principal.getName(), id) + "/sepay";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (org.springframework.dao.DataAccessException ex) {
            redirect.addFlashAttribute("error", "Chưa tạo được giao dịch. Vui lòng kiểm tra lịch sử trước khi thử lại.");
        }
        return "redirect:/member/memberships";
    }

    @GetMapping("/member/payments/{id}/sepay")
    public String checkout(@PathVariable Long id, Principal principal, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        try {
            model.addAttribute("order", payments.checkout(principal.getName(), id));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thanh toán VietQR.");
        }
        return "member/sepay-checkout";
    }

    @GetMapping("/member/payments/{id}/sepay/status")
    @ResponseBody
    public SepayPaymentService.Status status(@PathVariable Long id, Principal principal, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        try {
            return payments.status(principal.getName(), id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thanh toán VietQR.");
        }
    }
}
