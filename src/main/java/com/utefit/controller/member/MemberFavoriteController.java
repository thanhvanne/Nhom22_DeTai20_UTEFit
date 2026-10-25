package com.utefit.controller.member;

import com.utefit.enums.FavoriteType;
import com.utefit.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member/favorites")
public class MemberFavoriteController {
    private final FavoriteService favorites;
    private static final Set<String> RETURNS = Set.of("/trainers", "/classes", "/branches", "/member/favorites");

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("favorites", favorites.list(principal.getName()));
        return "member/favorites";
    }

    @PostMapping("/{type}/{targetId}")
    public String add(@PathVariable FavoriteType type, @PathVariable Long targetId,
                      @RequestParam(defaultValue = "/member/favorites") String returnTo,
                      Principal principal, RedirectAttributes redirect) {
        try {
            favorites.add(principal.getName(), type, targetId);
            redirect.addFlashAttribute("success", "Đã lưu yêu thích.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return destination(returnTo);
    }

    @PostMapping("/{id}/remove")
    public String remove(@PathVariable Long id, @RequestParam(defaultValue = "/member/favorites") String returnTo,
                         Principal principal, RedirectAttributes redirect) {
        favorites.remove(principal.getName(), id);
        redirect.addFlashAttribute("success", "Đã bỏ yêu thích.");
        return destination(returnTo);
    }

    private String destination(String returnTo) {
        return "redirect:" + (RETURNS.contains(returnTo) ? returnTo : "/member/favorites");
    }
}
