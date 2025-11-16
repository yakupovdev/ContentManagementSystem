package com.github.yakupovdev.cms.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PostResponse {

    private Long id;

    private String generatedDescription;

    private String originalDescription;

    private String hashtags;

    private String photoPath;

    private String size;

    private LocalDateTime createdAt;
}