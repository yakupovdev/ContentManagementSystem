package com.github.yakupovdev.cms.service;

import com.github.yakupovdev.cms.dto.PostRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpenAIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAIService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        log.info("OpenAI Service initialized with model: {}", model);
    }


    public String generatePostDescription(PostRequestDTO request) {
        log.info("Generating post description with GPT-4o");

        boolean hasPhoto = request.getPhoto() != null && !request.getPhoto().isEmpty();
        log.debug("Request - Size: {}, Hashtags: {}, Has photo: {}",
                request.getSize(), request.getHashtags(), hasPhoto);

        try {
            String prompt = buildPrompt(request);
            ObjectNode requestBody = buildRequestBody(prompt, request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> httpEntity = new HttpEntity<>(
                    requestBody.toString(),
                    headers
            );

            log.debug("Sending request to OpenAI API...");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.openai.com/v1/chat/completions",
                    httpEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                String content = extractContent(response.getBody());
                log.info("Successfully generated description (length: {} chars)",
                        content.length());
                return content;
            } else {
                log.error("OpenAI API error: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("OpenAI API returned status: " +
                        response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error generating description: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate post description: " +
                    e.getMessage(), e);
        }
    }


    private ObjectNode buildRequestBody(String prompt, PostRequestDTO request)
            throws Exception {

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.85);
        root.put("max_tokens", getMaxTokensBySize(request.getSize()));
        root.put("top_p", 0.95);
        root.put("frequency_penalty", 0.3);
        root.put("presence_penalty", 0.3);

        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", getSystemMessage());
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            ArrayNode content = objectMapper.createArrayNode();

            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("type", "text");
            textPart.put("text", prompt);
            content.add(textPart);

            ObjectNode imagePart = objectMapper.createObjectNode();
            imagePart.put("type", "image_url");

            ObjectNode imageUrl = objectMapper.createObjectNode();
            String base64Image = encodeImageToBase64(request.getPhoto());
            String mimeType = request.getPhoto().getContentType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                mimeType = "image/jpeg";
            }
            imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);

            imagePart.set("image_url", imageUrl);
            content.add(imagePart);

            userMessage.set("content", content);

            log.info("✅ Image included in request for GPT-4o Vision analysis");

        } else {
            userMessage.put("content", prompt);
            log.info("ℹ️  Text-only request (no image)");
        }

        messages.add(userMessage);
        root.set("messages", messages);

        return root;
    }


    private String encodeImageToBase64(MultipartFile photo) throws Exception {
        byte[] imageBytes = photo.getBytes();

        if (imageBytes.length > 20 * 1024 * 1024) {
            throw new RuntimeException("Image too large (max 20MB)");
        }

        log.debug("Encoding image: size={} bytes, type={}",
                imageBytes.length, photo.getContentType());

        return Base64.getEncoder().encodeToString(imageBytes);
    }


    private String extractContent(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText()
                .trim();
    }


    private String getSystemMessage() {
        return """
                You are a professional Instagram copywriter and social media expert.
                
                When analyzing images:
                - Describe what you see naturally
                - Focus on visual elements (colors, composition, mood, lighting)
                - Incorporate image details into the caption seamlessly
                - Connect visuals to emotions and storytelling
                
                Your writing style:
                - Engaging and emotionally resonant
                - Uses 2-5 emojis strategically
                - Natural and conversational
                - Includes clear call-to-action
                - Optimized for Instagram algorithm
                - Tells compelling stories
                
                Format:
                - Write in the user's language
                - Hashtags at the end, space-separated
                - Use line breaks for readability
                - Make it scroll-stopping and share-worthy
                """;
    }


    private String buildPrompt(PostRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create an Instagram post description");

        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            prompt.append(" based on the provided image");
        }
        prompt.append(".\n\n");

        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            prompt.append("📌 Context/Theme:\n");
            prompt.append(request.getDescription()).append("\n\n");
        }
        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            prompt.append("🏷 Required Hashtags:\n");
            List<String> formatted = request.getHashtags().stream()
                    .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                    .collect(Collectors.toList());
            prompt.append(String.join(" ", formatted)).append("\n\n");
        }

        prompt.append("📏 Length: ");
        prompt.append(getSizeDescription(request.getSize()));
        prompt.append("\n\n");

        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            prompt.append("✅ Requirements:\n");
            prompt.append("1. Analyze the image - describe what you see\n");
            prompt.append("2. Incorporate visual details naturally\n");
            prompt.append("3. Connect visuals to emotions and story\n");
            prompt.append("4. Use 2-5 emojis\n");
            prompt.append("5. Include call-to-action\n");
            prompt.append("6. Hashtags at the end\n");
        }

        prompt.append("\nGenerate now:");

        return prompt.toString();
    }

    private String getSizeDescription(PostRequestDTO.DescriptionSize size) {
        return switch (size) {
            case SHORT -> "SHORT (1-2 sentences, max 50 words)";
            case MEDIUM -> "MEDIUM (3-5 sentences, 50-150 words)";
            case LONG -> "LONG (6-10 sentences, 150-300 words)";
        };
    }

    private int getMaxTokensBySize(PostRequestDTO.DescriptionSize size) {
        return switch (size) {
            case SHORT -> 200;
            case MEDIUM -> 500;
            case LONG -> 1000;
        };
    }
}