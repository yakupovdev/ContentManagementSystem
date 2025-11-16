package com.github.yakupovdev.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class PostRequestDTO {

    private MultipartFile photo;

    @NotBlank(message = "Description is required")
    private String description;

    private List<String> hashtags;

    @NotNull(message = "Size is required")
    private DescriptionSize size;

    public enum DescriptionSize {
        SHORT,
        MEDIUM,
        LONG
    }
}