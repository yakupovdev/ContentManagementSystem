package com.github.yakupovdev.cms.controller;

import com.github.yakupovdev.cms.dto.UserInfoResponseDTO;
import com.github.yakupovdev.cms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDTO> getCurrentUser(Authentication authentication) {
        log.info("Fetching user info for: {}", authentication.getName());

        UserInfoResponseDTO userInfo = userService.getUserInfo(authentication.getName());

        return ResponseEntity.ok(userInfo);
    }

}