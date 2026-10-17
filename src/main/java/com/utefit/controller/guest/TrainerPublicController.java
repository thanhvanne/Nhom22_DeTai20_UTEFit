package com.utefit.controller.guest;

import com.utefit.dto.trainer.TrainerSearchRequest;
import com.utefit.service.TrainerService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/trainers")
public class TrainerPublicController {

    private final TrainerService trainerService;

    public TrainerPublicController(
            TrainerService trainerService
    ) {
        this.trainerService = trainerService;
    }

    @GetMapping
    public String list(
            @Valid @ModelAttribute("search")
            TrainerSearchRequest request,
            BindingResult bindingResult,
            Model model
    ) {

        if (bindingResult.hasErrors()) {
            request = new TrainerSearchRequest();
        }

        model.addAttribute(
                "search",
                request
        );

        model.addAttribute(
                "trainerPage",
                trainerService.searchPublicTrainers(request)
        );

        model.addAttribute(
                "branches",
                trainerService.getActiveBranchOptions()
        );

        return "guest/trainers";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "trainer",
                trainerService.getPublicTrainerById(id)
        );

        return "guest/trainer-detail";
    }
}