package com.utefit.controller.member;

import com.utefit.dto.notification.NotificationResponse;
import com.utefit.service.MemberNotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/member/notifications")
@RequiredArgsConstructor
public class MemberNotificationController {

    private final MemberNotificationService notifications;

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("notifications", notifications.getMyNotifications(principal.getName()));
        model.addAttribute("unreadCount", notifications.countUnread(principal.getName()));
        return "member/notifications";
    }

    @GetMapping("/recent")
    @ResponseBody
    public List<NotificationResponse> recent(Principal principal) {
        return notifications.getMyNotifications(principal.getName());
    }

    @PostMapping("/{id}/read")
    public String read(@PathVariable Long id, Principal principal,
                       RedirectAttributes redirect) {
        try {
            notifications.markRead(principal.getName(), id);
        } catch (EntityNotFoundException ex) {
            redirect.addFlashAttribute("errorMessage", "Thông báo không tồn tại.");
        }
        return "redirect:/member/notifications";
    }

    @PostMapping("/read-all")
    public String readAll(Principal principal) {
        notifications.markAllRead(principal.getName());
        return "redirect:/member/notifications";
    }
}