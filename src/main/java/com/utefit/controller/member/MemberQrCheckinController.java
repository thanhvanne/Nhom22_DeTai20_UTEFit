package com.utefit.controller.member;

import com.utefit.service.QrCheckinService;
import com.utefit.service.QrImageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member/qr-checkin")
public class MemberQrCheckinController {
    private final QrCheckinService tokens;
    private final QrImageService images;

    @GetMapping
    public String page(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "member/qr-checkin";
    }

    @PostMapping
    public String issue(Principal principal, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        try {
            var issued = tokens.issue(principal.getName());
            model.addAttribute("qrImage", images.dataUri(issued.value()));
            model.addAttribute("qrToken", issued.value());
            model.addAttribute("qrExpiresAt", issued.expiresAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "member/qr-checkin";
    }
}
