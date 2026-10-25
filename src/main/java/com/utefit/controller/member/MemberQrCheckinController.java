package com.utefit.controller.member;

import com.utefit.service.QrCheckinService;
import com.utefit.service.QrImageService;
import com.utefit.service.MemberQrStatusService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member/qr-checkin")
public class MemberQrCheckinController {
    private final QrCheckinService tokens;
    private final QrImageService images;
    private final MemberQrStatusService states;

    @GetMapping
    public String page(Principal principal, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        addStatus(principal, model, null);
        return "member/qr-checkin";
    }

    @PostMapping
    public String issue(Principal principal, Model model, HttpServletResponse response) {
        return issueQr(principal, model, response, false);
    }

    @PostMapping("/checkout")
    public String issueCheckout(Principal principal, Model model, HttpServletResponse response) {
        return issueQr(principal, model, response, true);
    }

    private String issueQr(Principal principal, Model model, HttpServletResponse response, boolean checkout) {
        response.setHeader("Cache-Control", "no-store");
        Long tokenId = null;
        try {
            var issued = checkout ? tokens.issueCheckout(principal.getName()) : tokens.issue(principal.getName());
            tokenId = issued.id();
            model.addAttribute("qrImage", images.dataUri(issued.value()));
            model.addAttribute("qrToken", issued.value());
            model.addAttribute("qrExpiresAt", issued.expiresAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
            model.addAttribute("qrTokenId", tokenId);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataAccessException | TransactionException ex) {
            model.addAttribute("error", "Chưa tạo được QR. Hãy cập nhật trạng thái rồi thử lại; không cần thay đổi dữ liệu Check-in.");
        }
        addStatus(principal, model, tokenId);
        return "member/qr-checkin";
    }

    private void addStatus(Principal principal, Model model, Long tokenId) {
        try {
            var state = states.status(principal.getName(), tokenId);
            model.addAttribute("checkinStatus", state);
            model.addAttribute("qrCheckoutAllowed", state != null && state.canCheckout());
            model.addAttribute("qrPurpose", state == null ? "NONE" : state.qrPurpose());
        } catch (DataAccessException | TransactionException ex) {
            model.addAttribute("syncError", "Tạm thời chưa đọc được trạng thái. Nhấn Thử lại / Cập nhật trạng thái để tiếp tục.");
        }
    }

    @GetMapping("/status")
    @ResponseBody
    public MemberQrStatusService.Status status(@RequestParam(required = false) Long tokenId,
                                               Principal principal, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        try {
            return states.status(principal.getName(), tokenId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy QR của bạn.");
        } catch (DataAccessException | TransactionException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Tạm thời chưa đọc được trạng thái Check-in.");
        }
    }
}
