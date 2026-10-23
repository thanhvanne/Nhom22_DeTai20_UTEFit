package com.utefit.controller.pt;

import com.utefit.service.CurrentAccountService;
import com.utefit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;
    private final CurrentAccountService accounts;

    @GetMapping
    public String listNotifications(Model model,Principal principal) {
        model.addAttribute("notifications",notifications.getNotificationsForUser(
                accounts.requireUserId(principal.getName())));
        return "notification/list";
    }
    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id,
                             @RequestParam(required=false) String redirect,Principal principal) {
        notifications.markAsRead(id,accounts.requireUserId(principal.getName()));
        return safeRedirect(redirect);
    }
    @PostMapping("/read-all")
    public String markAllAsRead(@RequestParam(required=false) String redirect,Principal principal) {
        notifications.markAllAsRead(accounts.requireUserId(principal.getName()));
        return safeRedirect(redirect);
    }
    @GetMapping("/unread-count") @ResponseBody
    public long unreadCount(Principal principal) {
        return notifications.countUnreadForUser(accounts.requireUserId(principal.getName()));
    }
    private String safeRedirect(String target) {
        return "redirect:"+(target!=null && (target.equals("/notifications")
                || target.equals("/pt/dashboard"))?target:"/notifications");
    }
}
