package com.github.yakupovdev.cms.controller;

import com.github.yakupovdev.cms.dto.*;
import com.github.yakupovdev.cms.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GeneratedPostDTO> generatePost(
            @Valid @ModelAttribute PostRequestDTO request,
            Authentication authentication) {

        log.info("Generate post request (without saving) from user: {}",
                authentication.getName());

        GeneratedPostDTO response = postService.generatePost(
                request,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/save")
    public ResponseEntity<PostResponseDTO> savePost(
            @Valid @RequestBody SavePostRequestDTO request,
            Authentication authentication) {

        log.info("Save post request from user: {}", authentication.getName());

        PostResponseDTO response = postService.saveGeneratedPost(
                request,
                authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping(value = "/generate-and-save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDTO> generateAndSavePost(
            @Valid @ModelAttribute PostRequestDTO request,
            Authentication authentication) {

        log.info("Generate and save post request from user: {}",
                authentication.getName());

        PostResponseDTO response = postService.generateAndSavePost(
                request,
                authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<List<PostResponseDTO>> getUserPosts(Authentication authentication) {
        log.info("Get posts request from user: {}", authentication.getName());

        List<PostResponseDTO> posts = postService.getUserPosts(authentication.getName());

        return ResponseEntity.ok(posts);
    }


    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDTO> getPostById(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Get post {} request from user: {}", id, authentication.getName());

        PostResponseDTO post = postService.getPostById(id, authentication.getName());

        return ResponseEntity.ok(post);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Delete post {} request from user: {}", id, authentication.getName());

        postService.deletePost(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getPostPhoto(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Get photo for post {} by user: {}", id, authentication.getName());

        try {
            PostResponseDTO post = postService.getPostById(id, authentication.getName());

            if (post.getPhotoPath() == null || post.getPhotoPath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path photoPath = Paths.get(post.getPhotoPath());
            Resource resource = new UrlResource(photoPath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = "image/jpeg";
                String filename = photoPath.getFileName().toString();
                if (filename.endsWith(".png")) {
                    contentType = "image/png";
                } else if (filename.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (filename.endsWith(".webp")) {
                    contentType = "image/webp";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                log.warn("Photo file not found or not readable: {}", post.getPhotoPath());
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error retrieving photo: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}