package com.telco.backend.controller;

import com.telco.backend.dto.UserProfileDTO;
import com.telco.backend.model.Customer;
import com.telco.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
// 🎯 MADDE 1 ÇÖZÜMÜ: Zafiyet yaratan @CrossOrigin silindi kanka! Artık merkezi SecurityConfig kuralları geçerli. ✅
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileDTO getMyProfile(@AuthenticationPrincipal Customer currentCustomer) {
        return userService.getProfile(currentCustomer);
    }

    @PutMapping("/me")
    public UserProfileDTO updateMyProfile(
            @AuthenticationPrincipal Customer currentCustomer,
            @RequestBody UserProfileDTO dto) {
        return userService.updateProfile(currentCustomer, dto);
    }
}