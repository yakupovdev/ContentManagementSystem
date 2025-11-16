package com.github.yakupovdev.cms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_description", columnDefinition = "TEXT")
    private String originalDescription;

    @Column(name = "generated_description", columnDefinition = "TEXT", nullable = false)
    private String generatedDescription;

    @Column(name = "hashtags", length = 500)
    private String hashtags;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @Column(name = "size", length = 20)
    private String size;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}