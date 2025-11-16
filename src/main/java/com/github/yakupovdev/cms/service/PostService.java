package com.github.yakupovdev.cms.service;

import com.github.yakupovdev.cms.dto.*;
import com.github.yakupovdev.cms.entity.Post;
import com.github.yakupovdev.cms.entity.User;
import com.github.yakupovdev.cms.repository.PostRepository;
import com.github.yakupovdev.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final OpenAIService openAIService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Value("${upload.temp.directory}")
    private String tempUploadDirectory;


    public GeneratedPostDTO generatePost(PostRequestDTO request, String username) {
        log.info("Generating post (without saving) for user: {}", username);

        String tempPhotoPath = null;
        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            try {
                tempPhotoPath = saveTempPhoto(request.getPhoto());
                log.info("Photo saved to temp: {}", tempPhotoPath);
            } catch (IOException e) {
                log.error("Failed to save temp photo: {}", e.getMessage());
                throw new RuntimeException("Failed to save photo: " + e.getMessage());
            }
        }

        String generatedDescription = openAIService.generatePostDescription(request);
        log.info("Description generated successfully");

        String hashtags = formatHashtags(request.getHashtags());

        return GeneratedPostDTO.builder()
                .generatedDescription(generatedDescription)
                .originalDescription(request.getDescription())
                .hashtags(hashtags)
                .tempPhotoPath(tempPhotoPath)
                .size(request.getSize().name())
                .generatedAt(LocalDateTime.now())
                .build();
    }


    @Transactional
    public PostResponseDTO saveGeneratedPost(SavePostRequestDTO request, String username) {
        log.info("Saving generated post for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String photoPath = null;
        if (request.getTempPhotoPath() != null && !request.getTempPhotoPath().isEmpty()) {
            try {
                photoPath = moveTempPhotoToPermanent(request.getTempPhotoPath());
                log.info("Photo moved to permanent storage: {}", photoPath);
            } catch (IOException e) {
                log.error("Failed to move photo: {}", e.getMessage());
                throw new RuntimeException("Failed to save photo: " + e.getMessage());
            }
        }

        Post post = Post.builder()
                .userId(user.getId())
                .originalDescription(request.getOriginalDescription())
                .generatedDescription(request.getGeneratedDescription())
                .hashtags(request.getHashtags())
                .photoPath(photoPath)
                .size(request.getSize())
                .build();

        post = postRepository.save(post);
        log.info("Post saved to database with ID: {}", post.getId());

        return mapToResponse(post);
    }


    @Transactional
    public PostResponseDTO generateAndSavePost(PostRequestDTO request, String username) {
        log.info("Generating and saving post for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String photoPath = null;
        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            try {
                photoPath = savePhoto(request.getPhoto());
                log.info("Photo saved: {}", photoPath);
            } catch (IOException e) {
                log.error("Failed to save photo: {}", e.getMessage());
                throw new RuntimeException("Failed to save photo: " + e.getMessage());
            }
        }

        String generatedDescription = openAIService.generatePostDescription(request);
        log.info("Description generated successfully");

        String hashtags = formatHashtags(request.getHashtags());

        Post post = Post.builder()
                .userId(user.getId())
                .originalDescription(request.getDescription())
                .generatedDescription(generatedDescription)
                .hashtags(hashtags)
                .photoPath(photoPath)
                .size(request.getSize().name())
                .build();

        post = postRepository.save(post);
        log.info("Post saved to database with ID: {}", post.getId());

        return mapToResponse(post);
    }


    @Transactional(readOnly = true)
    public List<PostResponseDTO> getUserPosts(String username) {
        log.info("Fetching posts for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        log.info("Found {} posts for user: {}", posts.size(), username);

        return posts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public PostResponseDTO getPostById(Long postId, String username) {
        log.info("Fetching post {} for user: {}", postId, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Post post = postRepository.findByIdAndUserId(postId, user.getId());

        if (post == null) {
            throw new RuntimeException("Post not found or access denied");
        }

        return mapToResponse(post);
    }


    @Transactional
    public void deletePost(Long postId, String username) {
        log.info("Deleting post {} for user: {}", postId, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Post post = postRepository.findByIdAndUserId(postId, user.getId());

        if (post == null) {
            throw new RuntimeException("Post not found or access denied");
        }

        if (post.getPhotoPath() != null && !post.getPhotoPath().isEmpty()) {
            try {
                Path photoPath = Paths.get(post.getPhotoPath());
                Files.deleteIfExists(photoPath);
                log.info("Photo deleted: {}", post.getPhotoPath());
            } catch (IOException e) {
                log.warn("Failed to delete photo: {}", e.getMessage());
            }
        }

        postRepository.delete(post);
        log.info("Post deleted successfully: {}", postId);
    }


    private String saveTempPhoto(MultipartFile photo) throws IOException {
        Path tempPath = Paths.get(tempUploadDirectory);

        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
            log.info("Created temp upload directory: {}", tempPath);
        }

        String originalFilename = photo.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = "temp_" + UUID.randomUUID().toString() + extension;
        Path filePath = tempPath.resolve(filename);

        Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Temp photo saved: filename={}, size={} bytes",
                filename, photo.getSize());

        return filePath.toString();
    }


    private String moveTempPhotoToPermanent(String tempPhotoPath) throws IOException {
        Path tempPath = Paths.get(tempPhotoPath);

        if (!Files.exists(tempPath)) {
            throw new IOException("Temp photo not found: " + tempPhotoPath);
        }

        Path uploadPath = Paths.get(uploadDirectory);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String filename = tempPath.getFileName().toString().replace("temp_", "");
        Path permanentPath = uploadPath.resolve(filename);

        Files.move(tempPath, permanentPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Photo moved: {} -> {}", tempPath, permanentPath);

        return permanentPath.toString();
    }


    private String savePhoto(MultipartFile photo) throws IOException {
        Path uploadPath = Paths.get(uploadDirectory);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath);
        }

        String originalFilename = photo.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Photo saved to disk: filename={}, size={} bytes",
                filename, photo.getSize());

        return filePath.toString();
    }


    private String formatHashtags(List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty()) {
            return "";
        }

        return hashtags.stream()
                .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                .collect(Collectors.joining(" "));
    }


    private PostResponseDTO mapToResponse(Post post) {
        return PostResponseDTO.builder()
                .id(post.getId())
                .generatedDescription(post.getGeneratedDescription())
                .originalDescription(post.getOriginalDescription())
                .hashtags(post.getHashtags())
                .photoPath(post.getPhotoPath())
                .size(post.getSize())
                .createdAt(post.getCreatedAt())
                .build();
    }
}