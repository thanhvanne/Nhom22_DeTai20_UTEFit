package com.utefit.controller.pt;

import com.utefit.dto.notification.NotificationDto;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import com.utefit.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public String listNotifications(Model model, Principal principal) {
        Long userId = getCurrentUserId(principal);
        List<NotificationDto> notifications =
                notificationService.getNotificationsForUser(userId);
        model.addAttribute("notifications", notifications);
        return "notification/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable("id") Long id,
                             @RequestParam(value = "redirect", required = false) String redirect,
                             Principal principal) {
        Long userId = getCurrentUserId(principal);
        notificationService.markAsRead(id, userId);
        return "redirect:" + (redirect != null ? redirect : "/notifications");
    }

    @PostMapping("/read-all")
    public String markAllAsRead(@RequestParam(value = "redirect", required = false) String redirect,
                                Principal principal) {
        Long userId = getCurrentUserId(principal);
        notificationService.markAllAsRead(userId);
        return "redirect:" + (redirect != null ? redirect : "/notifications");
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public long unreadCount(Principal principal) {
        Long userId = getCurrentUserId(principal);
        return notificationService.countUnreadForUser(userId);
    }

    private Long getCurrentUserId(Principal principal) {
        if (principal == null) {
            throw new EntityNotFoundException("Không tìm thấy người dùng đăng nhập.");
        }
        String email = principal.getName(); // username = email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy người dùng với email: " + email));
        return user.getId();
    }
}