package com.github.yakupovdev.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SavePostRequestDTO {

    @NotBlank(message = "Generated description is required")
    private String generatedDescription;

    private String originalDescription;

    private String hashtags;


    private String tempPhotoPath;

    @NotBlank(message = "Size is required")
    private String size;
}