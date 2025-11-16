package com.github.yakupovdev.cms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserInfoResponse {

    private Long id;

    private String username;

    private LocalDateTime createdAt;

    private Long totalPosts;
}