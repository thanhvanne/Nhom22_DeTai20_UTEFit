package com.utefit.controller.pt;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/pt/profile")
public class PtProfileController {

    @GetMapping
    public String profile() {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Chức năng yêu cầu đăng nhập PT"
        );
    }

    @GetMapping("/edit")
    public String editForm() {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Chức năng yêu cầu đăng nhập PT"
        );
    }

    @PostMapping("/edit")
    public String update() {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Chức năng yêu cầu đăng nhập PT"
        );
    }
}