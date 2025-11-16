package com.github.yakupovdev.cms.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GeneratedPostDTO {

    private String generatedDescription;

    private String originalDescription;

    private String hashtags;

    private String tempPhotoPath;

    private String size;

    private LocalDateTime generatedAt;
}