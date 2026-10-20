package com.utefit.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        String password = "123456789";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);

        System.out.println("Mat khau: " + password);
        System.out.println("BCrypt hash: " + hash);
        System.out.println("Kiem tra dung mat khau: "
                + encoder.matches(password, hash));
    }
}