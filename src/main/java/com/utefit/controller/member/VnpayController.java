package com.utefit.controller.member;

import com.utefit.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequiredArgsConstructor
public class VnpayController {
    private final VnpayPaymentService payments;

    @PostMapping("/member/memberships/{id}/vnpay")
    public String start(@PathVariable Long id, Principal principal, HttpServletRequest request,
                        RedirectAttributes redirect) {
        try {
            return "redirect:" + payments.start(principal.getName(), id, request.getRemoteAddr());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/member/memberships";
        }
    }

    @GetMapping("/payments/vnpay/return")
    public String result(@RequestParam MultiValueMap<String, String> fields, Model model) {
        model.addAttribute("message", payments.returnMessage(singleValues(fields)));
        return "member/vnpay-result";
    }

    @GetMapping("/payments/vnpay/ipn")
    @ResponseBody
    public Map<String, String> ipn(@RequestParam MultiValueMap<String, String> fields) {
        try {
            return payments.ipn(singleValues(fields));
        } catch (RuntimeException ex) {
            return Map.of("RspCode", "99", "Message", "Unable to confirm payment");
        }
    }

    private Map<String, String> singleValues(MultiValueMap<String, String> fields) {
        if (fields.entrySet().stream().anyMatch(entry -> entry.getValue().size() != 1)) return Map.of();
        return new TreeMap<>(fields.toSingleValueMap());
    }
}
