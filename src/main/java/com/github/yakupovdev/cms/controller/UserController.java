package com.github.yakupovdev.cms.controller;

import com.github.yakupovdev.cms.dto.UserInfoResponse;
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
    public ResponseEntity<UserInfoResponse> getCurrentUser(Authentication authentication) {
        log.info("Fetching user info for: {}", authentication.getName());

        UserInfoResponse userInfo = userService.getUserInfo(authentication.getName());

        return ResponseEntity.ok(userInfo);
    }

}