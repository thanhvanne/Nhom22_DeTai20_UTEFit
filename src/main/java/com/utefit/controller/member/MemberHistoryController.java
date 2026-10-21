package com.utefit.controller.member;

import com.utefit.service.MemberHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/member/history")
@RequiredArgsConstructor
public class MemberHistoryController {

    private final MemberHistoryService history;

    @GetMapping
    public String list(
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "history",
                history.getHistory(principal.getName())
        );

        return "member/history";
    }
}