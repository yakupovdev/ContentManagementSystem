package com.github.yakupovdev.cms.service;

import com.github.yakupovdev.cms.dto.PostRequest;
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


    public String generatePostDescription(PostRequest request) {
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


    private ObjectNode buildRequestBody(String prompt, PostRequest request)
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