package com.utefit.controller.member;

import com.utefit.controller.guest.TrainerPublicController;
import com.utefit.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Map;

@ControllerAdvice(assignableTypes = {PublicCatalogController.class, TrainerPublicController.class})
@RequiredArgsConstructor
public class FavoriteCatalogAdvice {
    private final FavoriteService favorites;

    @ModelAttribute("savedFavorites")
    public Map<String, Long> saved(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_MEMBER"))) {
            return Map.of();
        }
        return favorites.saved(authentication.getName());
    }
}
